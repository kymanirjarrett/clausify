const dateTime = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' });
const dateOnly = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' });

export function formatDateTime(iso: string | null | undefined): string {
  return iso ? dateTime.format(new Date(iso)) : '';
}

export function formatDate(iso: string): string {
  return dateOnly.format(new Date(iso));
}

/** Drawing-number style identifier shown next to each contract. */
export function contractNumber(id: number): string {
  return `CL-${String(id).padStart(5, '0')}`;
}
