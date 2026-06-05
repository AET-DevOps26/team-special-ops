export function SeasonTabs({
  seasons,
  selected,
  current,
  onSelect,
}: {
  seasons: number[]
  selected: number | null
  current: number | null
  onSelect: (season: number) => void
}) {
  return (
    <div className="mt-6 flex flex-wrap gap-2" role="tablist" aria-label="Seasons">
      {seasons.map((s) => {
        const active = s === selected
        return (
          <button
            key={s}
            type="button"
            role="tab"
            aria-selected={active}
            onClick={() => onSelect(s)}
            className={
              active
                ? 'rounded-md bg-slate-900 px-3 py-1.5 text-sm font-medium text-white'
                : 'rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-100'
            }
          >
            Season {s}
            {s === current && (
              <span className="ml-1 text-emerald-500" aria-label="current season">
                •
              </span>
            )}
          </button>
        )
      })}
    </div>
  )
}
