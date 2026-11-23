# TV scrolling: research and implementation

## Official guidance consulted

- [Scrollable TV layouts](https://developer.android.com/training/tv/playback/compose/lists): standard Foundation lazy layouts, supported since Foundation 1.7, own focus-driven scrolling. Use `BringIntoViewSpec` for a consistent pivot instead of deprecated TV lazy layouts or competing per-focus animations.
- [Compose for TV](https://developer.android.com/training/tv/playback/compose): use TV Material controls for clear remote focus feedback; standard mobile Material behavior is not optimized for TVs.
- [TV navigation](https://developer.android.com/training/tv/start/navigation): keep directional movement predictable, leave space for focus highlights, and make every action reachable with the D-pad.
- [Focus behavior](https://developer.android.com/develop/ui/compose/touch-input/focus/change-focus-behavior): request initial focus in a side effect and restore the last focused child when returning to a container.

## Vista changes

- Replaced manual focus-triggered `animateScrollToItem` jobs with one `TvFocusScrolling` provider around the TV navigation graph. Standard lazy rows/columns/grids now handle focus motion, avoiding two competing scroll owners and animation jobs per remote repeat.
- Use a leading-third pivot, with edge fitting for large cards and safe handling of unmeasured geometry. This is a consistent focus line, not a promise that all boundary items can scroll beyond content bounds.
- Stable section/media keys, saved last-focused card and row/grid `focusRestorer` groups preserve context. A fresh Home starts with Search focused.
- Add vertical padding around focusable cards/chips and a higher drawing order for the focused card; remove compounded Surface/custom focus scaling and use a non-bouncy focus transition.
- TV search uses adaptive 160 dp grid cells instead of a fixed five-column layout.

## Verification boundary

Unit tests exercise pivot positioning, trailing-edge visibility, oversized cards and invalid geometry. Compilation confirms API compatibility with this project's existing Compose BOM. No Android TV is attached to ADB in this session. Hold-to-scroll, returning from Details, RTL, large fonts, overscan and D-pad-only accessibility need device verification before claiming a visual/device pass.
