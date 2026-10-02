import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, afterNextRender, inject, input, viewChild,
} from '@angular/core';

/**
 * The landing hero's live drafting field (WebGL2): a construction grid that sweeps in, bends under the
 * cursor like a loupe over the drawing, and sends faint tolerance rings out from the callout's anchor.
 * Static single frame with reduced motion; plain CSS grid if WebGL is unavailable. Purely decorative.
 */
const VERTEX = `#version 300 es
in vec2 aPos;
void main() { gl_Position = vec4(aPos, 0.0, 1.0); }`;

const FRAGMENT = `#version 300 es
precision highp float;
uniform vec2 uRes;      // canvas size, device pixels
uniform float uDpr;
uniform float uTime;
uniform float uReveal;  // 0..1 sweep-in
uniform vec2 uMouse;    // css px, top-left origin; negative when absent
uniform vec2 uAnchor;   // css px; negative when absent
out vec4 outColor;

const vec3 VELLUM = vec3(0.949, 0.957, 0.945);
const vec3 BLUE = vec3(0.204, 0.376, 0.820);
const vec3 AMBER = vec3(0.843, 0.514, 0.165);

float gridLine(float coord, float spacing, float width) {
  float d = abs(fract(coord / spacing - 0.5) - 0.5) * spacing;
  return 1.0 - smoothstep(width * 0.5, width * 0.5 + 0.9, d);
}

void main() {
  vec2 size = uRes / uDpr;
  vec2 p = vec2(gl_FragCoord.x, uRes.y - gl_FragCoord.y) / uDpr;

  // Loupe: the grid is pulled toward the cursor, so the cells under it magnify.
  float lens = 0.0;
  if (uMouse.x >= 0.0) {
    vec2 d = p - uMouse;
    lens = exp(-dot(d, d) / (2.0 * 150.0 * 150.0));
    p -= d * lens * 0.22;
  }

  float minor = max(gridLine(p.x, 24.0, 1.0), gridLine(p.y, 24.0, 1.0));
  float major = max(gridLine(p.x, 120.0, 1.0), gridLine(p.y, 120.0, 1.0));
  float ink = max(minor * 0.09, major * 0.2) * (1.0 + lens * 1.6);

  // Sweep-in along a shallow diagonal, like a pen ruling the sheet.
  float sweep = (p.x + p.y * 0.4) / (size.x + size.y * 0.4);
  ink *= smoothstep(sweep - 0.02, sweep + 0.06, uReveal * 1.1);

  // Calm toward the reading column on the left so text stays crisp.
  ink *= mix(0.45, 1.0, smoothstep(size.x * 0.12, size.x * 0.55, p.x));

  vec3 color = mix(VELLUM, BLUE, clamp(ink, 0.0, 1.0));

  // Tolerance rings drifting out from the callout anchor.
  if (uAnchor.x >= 0.0) {
    float r = length(p - uAnchor);
    float ring = gridLine(r - uTime * 12.0, 42.0, 1.1) * smoothstep(380.0, 40.0, r) * smoothstep(0.0, 26.0, r);
    color = mix(color, AMBER, ring * 0.32 * smoothstep(0.75, 1.0, uReveal));
  }

  outColor = vec4(color, 1.0);
}`;

@Component({
  selector: 'cl-hero-field',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'absolute inset-0 overflow-hidden', 'aria-hidden': 'true' },
  template: `<canvas #canvas class="block size-full"></canvas>`,
  styles: `:host { background-color: var(--color-vellum);
    background-image: linear-gradient(rgb(52 96 209 / .12) 1px, transparent 1px),
      linear-gradient(90deg, rgb(52 96 209 / .12) 1px, transparent 1px);
    background-size: 120px 120px; }
    canvas { opacity: 0; transition: opacity 400ms var(--ease-out-expo); }
    canvas.ready { opacity: 1; }`,
})
export class HeroField {
  /** Element whose center the tolerance rings radiate from (the callout's anchor point). */
  readonly anchor = input<HTMLElement | undefined>();

  private readonly canvasRef = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    afterNextRender(() => this.start());
  }

  private start(): void {
    const canvas = this.canvasRef().nativeElement;
    const gl = canvas.getContext('webgl2', { antialias: false, premultipliedAlpha: false });
    if (!gl) {
      return; // CSS grid fallback stays visible.
    }
    const program = link(gl, VERTEX, FRAGMENT);
    if (!program) {
      return;
    }
    const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;

    const buffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
    const aPos = gl.getAttribLocation(program, 'aPos');
    gl.enableVertexAttribArray(aPos);
    gl.vertexAttribPointer(aPos, 2, gl.FLOAT, false, 0, 0);
    gl.useProgram(program);
    const u = (name: string) => gl.getUniformLocation(program, name);
    const [uRes, uDpr, uTime, uReveal, uMouse, uAnchor] =
      ['uRes', 'uDpr', 'uTime', 'uReveal', 'uMouse', 'uAnchor'].map(u);

    let dpr = 1;
    let anchor = [-1, -1];
    const measureAnchor = () => {
      const el = this.anchor();
      if (!el) return;
      const rect = canvas.getBoundingClientRect();
      const a = el.getBoundingClientRect();
      anchor = [a.left + a.width / 2 - rect.left, a.top + a.height / 2 - rect.top];
    };
    const target = [-1, -1];
    const mouse = [-1, -1];
    const resize = () => {
      dpr = Math.min(devicePixelRatio || 1, 2);
      const rect = canvas.getBoundingClientRect();
      canvas.width = Math.max(1, Math.round(rect.width * dpr));
      canvas.height = Math.max(1, Math.round(rect.height * dpr));
      gl.viewport(0, 0, canvas.width, canvas.height);
      measureAnchor();
    };
    const onPointer = (e: PointerEvent) => {
      const rect = canvas.getBoundingClientRect();
      target[0] = e.clientX - rect.left;
      target[1] = e.clientY - rect.top;
      if (mouse[0] < 0) {
        mouse[0] = target[0];
        mouse[1] = target[1];
      }
    };
    const onLeave = () => { target[0] = target[1] = mouse[0] = mouse[1] = -1; };

    const startedAt = performance.now();
    let frame = 0;
    let visible = true;
    const draw = (now: number) => {
      const t = (now - startedAt) / 1000;
      // The plate settles in and fonts swap after start, so track the marker as it moves.
      measureAnchor();
      const reveal = reducedMotion ? 1 : 1 - Math.pow(1 - Math.min(t / 1.8, 1), 4);
      if (target[0] >= 0) {
        mouse[0] += (target[0] - mouse[0]) * 0.12;
        mouse[1] += (target[1] - mouse[1]) * 0.12;
      }
      gl.uniform2f(uRes, canvas.width, canvas.height);
      gl.uniform1f(uDpr, dpr);
      gl.uniform1f(uTime, reducedMotion ? 0 : t);
      gl.uniform1f(uReveal, reveal);
      gl.uniform2f(uMouse, mouse[0], mouse[1]);
      gl.uniform2f(uAnchor, anchor[0], anchor[1]);
      gl.drawArrays(gl.TRIANGLES, 0, 3);
      if (!reducedMotion && visible) {
        frame = requestAnimationFrame(draw);
      }
    };

    resize();
    const resizeObserver = new ResizeObserver(() => {
      resize();
      if (reducedMotion) draw(performance.now());
    });
    resizeObserver.observe(canvas);
    const intersection = new IntersectionObserver(([entry]) => {
      visible = entry.isIntersecting;
      cancelAnimationFrame(frame);
      if (visible && !reducedMotion) frame = requestAnimationFrame(draw);
    });
    intersection.observe(canvas);
    if (!reducedMotion) {
      window.addEventListener('pointermove', onPointer, { passive: true });
      document.documentElement.addEventListener('pointerleave', onLeave);
    }
    draw(performance.now());
    canvas.classList.add('ready');

    this.destroyRef.onDestroy(() => {
      cancelAnimationFrame(frame);
      resizeObserver.disconnect();
      intersection.disconnect();
      window.removeEventListener('pointermove', onPointer);
      document.documentElement.removeEventListener('pointerleave', onLeave);
      gl.getExtension('WEBGL_lose_context')?.loseContext();
    });
  }
}

function link(gl: WebGL2RenderingContext, vertex: string, fragment: string): WebGLProgram | null {
  const compile = (type: number, source: string) => {
    const shader = gl.createShader(type)!;
    gl.shaderSource(shader, source);
    gl.compileShader(shader);
    return gl.getShaderParameter(shader, gl.COMPILE_STATUS) ? shader : null;
  };
  const vs = compile(gl.VERTEX_SHADER, vertex);
  const fs = compile(gl.FRAGMENT_SHADER, fragment);
  if (!vs || !fs) return null;
  const program = gl.createProgram()!;
  gl.attachShader(program, vs);
  gl.attachShader(program, fs);
  gl.linkProgram(program);
  return gl.getProgramParameter(program, gl.LINK_STATUS) ? program : null;
}
