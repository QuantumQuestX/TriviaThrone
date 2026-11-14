# Mod UI styles

Game mods can use the host's shared stylesheet for common layout and controls. Include it with:

```html
<link rel="stylesheet" href="/css/mod-api.css">
```

Built-in pages load the stylesheet through `root.css`. The API is opt-in and uses the `tt-` prefix; it does not style HTML elements globally. Mods can keep their own layouts and override `--tt-*` properties in their stylesheet.

The classes and properties listed below are the public interface. More may be added; existing names are intended to remain compatible.

## Theme tokens

Use these properties to adjust the default colors and sizing:

| Property                                                                                               | Purpose                               |
|--------------------------------------------------------------------------------------------------------|---------------------------------------|
| `--tt-color-canvas`, `--tt-color-surface`, `--tt-color-surface-raised`, `--tt-color-surface-hover`     | Page and panel surfaces               |
| `--tt-color-text`, `--tt-color-muted`, `--tt-color-border`, `--tt-color-border-strong`                 | Text and borders                      |
| `--tt-color-accent`, `--tt-color-accent-hover`, `--tt-color-accent-subtle`                             | Primary actions, focus, and selection |
| `--tt-color-danger`, `--tt-color-danger-hover`, `--tt-color-danger-active`, `--tt-color-danger-subtle` | Destructive actions                   |
| `--tt-color-success`, `--tt-color-success-subtle`                                                      | Positive actions and status           |
| `--tt-radius-sm`, `--tt-radius-md`, `--tt-radius-lg`                                                   | Corner radii                          |
| `--tt-space-1`, `--tt-space-2`, `--tt-space-3`, `--tt-space-4`, `--tt-space-6`, `--tt-space-8`         | Spacing scale                         |
| `--tt-font-family`, `--tt-shadow-card`                                                                 | Typography and card shadow            |

## Classes

| Class                                                                          | Purpose                                               |
|--------------------------------------------------------------------------------|-------------------------------------------------------|
| `tt-shell`                                                                     | Page background, text color, font, and minimum height |
| `tt-container`                                                                 | Centered responsive content width                     |
| `tt-header`, `tt-header__inner`                                                | Header surface and aligned inner row                  |
| `tt-brand`, `tt-link`                                                          | Brand and accessible link styles                      |
| `tt-main`                                                                      | Main content spacing                                  |
| `tt-card`, `tt-card__header`                                                   | Surface panel and divided heading                     |
| `tt-grid`, `tt-grid--2`, `tt-grid__full`                                       | Responsive grid and full-width grid item              |
| `tt-field`, `tt-field--checkbox`, `tt-field__label`, `tt-field__hint`          | Form field, checkbox row, label, and supporting text  |
| `tt-control`, `tt-checkbox`                                                    | Inputs/selects and checkbox                           |
| `tt-button`, `tt-button--primary`, `tt-button--secondary`, `tt-button--danger` | Button base and variants                              |
| `tt-status`, `tt-status--success`, `tt-status--error`, `tt-status--info`       | Feedback messages                                     |
| `tt-muted`                                                                     | Secondary text                                        |

## Example

```html
<body class="tt-shell">
    <header class="tt-header">
        <div class="tt-container tt-header__inner">
            <a class="tt-brand" href="/">TriviaThrone</a>
            <a class="tt-link" href="/">Dashboard</a>
        </div>
    </header>
    <main class="tt-container tt-main">
        <section class="tt-card">
            <div class="tt-card__header">
                <h1>My game</h1>
            </div>
            <div class="tt-field">
                <label class="tt-field__label" for="title">Title</label>
                <input class="tt-control" id="title" name="title">
                <small class="tt-field__hint">Shown to players in the lobby.</small>
            </div>
            <button class="tt-button tt-button--primary">Save game</button>
        </section>
    </main>
</body>
```

The stylesheet provides reusable styles, not a required page layout or game-specific design.
