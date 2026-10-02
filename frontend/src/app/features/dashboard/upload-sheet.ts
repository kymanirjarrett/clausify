import { HttpEventType } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { ContractSummary } from '../../core/api.models';
import { ContractsService, formatBytes, validatePdf } from '../../core/contracts.service';
import { toApiError } from '../../core/problem';
import { NgTemplateOutlet } from '@angular/common';
import { Icon } from '../../shared/icon';

type UploadState =
  | { kind: 'idle' }
  | { kind: 'uploading'; file: File; percent: number }
  | { kind: 'reading'; file: File }
  | { kind: 'error'; file?: File; message: string };

/** FR-3: drop or browse for one PDF. Checks type and size first, then shows real upload progress. */
@Component({
  selector: 'cl-upload-sheet',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon, NgTemplateOutlet],
  templateUrl: './upload-sheet.html',
  styleUrl: './upload-sheet.css',
})
export class UploadSheet {
  readonly uploaded = output<ContractSummary>();

  private readonly contracts = inject(ContractsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly input = viewChild.required<ElementRef<HTMLInputElement>>('fileInput');
  private subscription?: Subscription;
  private dragDepth = 0;

  protected readonly state = signal<UploadState>({ kind: 'idle' });
  protected readonly dragging = signal(false);
  protected readonly formatBytes = formatBytes;

  protected browse(): void {
    if (!this.busy()) this.input().nativeElement.click();
  }

  protected busy(): boolean {
    const kind = this.state().kind;
    return kind === 'uploading' || kind === 'reading';
  }

  protected onPicked(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (files?.length) this.start(files);
    input.value = '';
  }

  protected onDragEnter(event: DragEvent): void {
    event.preventDefault();
    this.dragDepth++;
    if (!this.busy()) this.dragging.set(true);
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer) event.dataTransfer.dropEffect = this.busy() ? 'none' : 'copy';
  }

  protected onDragLeave(): void {
    this.dragDepth = Math.max(0, this.dragDepth - 1);
    if (this.dragDepth === 0) this.dragging.set(false);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragDepth = 0;
    this.dragging.set(false);
    const files = event.dataTransfer?.files;
    if (files?.length && !this.busy()) this.start(files);
  }

  protected cancel(): void {
    this.subscription?.unsubscribe();
    this.state.set({ kind: 'idle' });
  }

  protected dismiss(): void {
    this.state.set({ kind: 'idle' });
  }

  private start(files: FileList): void {
    if (files.length > 1) {
      this.state.set({ kind: 'error', message: 'Upload one contract at a time.' });
      return;
    }
    const file = files[0];
    const problem = validatePdf(file);
    if (problem) {
      this.state.set({ kind: 'error', file, message: problem });
      return;
    }
    this.state.set({ kind: 'uploading', file, percent: 0 });
    this.subscription = this.contracts.upload(file).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress) {
          const percent = event.total ? Math.round((event.loaded / event.total) * 100) : 0;
          this.state.set(percent >= 100 ? { kind: 'reading', file } : { kind: 'uploading', file, percent });
        } else if (event.type === HttpEventType.Response && event.body) {
          this.state.set({ kind: 'idle' });
          this.uploaded.emit(event.body);
        }
      },
      error: (error) => this.state.set({ kind: 'error', file, message: toApiError(error).message }),
    });
  }
}
