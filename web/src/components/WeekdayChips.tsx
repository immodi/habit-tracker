import { ALL_WEEKDAYS, weekdayLabel } from '../lib/dates';

interface WeekdayChipsProps {
  value: number[];
  onChange: (next: number[]) => void;
}

export function WeekdayChips({ value, onChange }: WeekdayChipsProps) {
  return (
    <div className="chips" role="group" aria-label="Scheduled days">
      {ALL_WEEKDAYS.map((day) => {
        const selected = value.includes(day);
        return (
          <button
            key={day}
            type="button"
            className="chip"
            aria-pressed={selected}
            onClick={() =>
              onChange(
                selected ? value.filter((entry) => entry !== day) : [...value, day],
              )
            }
          >
            {weekdayLabel(day)}
          </button>
        );
      })}
    </div>
  );
}
