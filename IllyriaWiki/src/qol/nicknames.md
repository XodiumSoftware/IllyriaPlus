# Nicknames

Change your in-game display name with **MiniMessage** formatting support.

## Usage <img class="mc-icon" alt="" src="../img/item/name_tag.png">

Run `/nick` (alias for `/nickname`) to open the nickname dialog.
Enter your desired name and confirm.

## MiniMessage Formatting

Nicknames support full MiniMessage syntax — colors, gradients, bold, and more.

### Examples

| Input                                       | Result                                                                                                                                             |
| ------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| `<red>Steve</red>`                          | <span style="color:#FF5555">Steve</span>                                                                                                           |
| `<gradient:#FF0000:#0000FF>Alex</gradient>` | <span style="background:linear-gradient(to right,#FF0000,#0000FF);-webkit-background-clip:text;background-clip:text;color:transparent">Alex</span> |
| `<bold><gold>Knight</gold></bold>`          | <span style="color:#FFAA00;font-weight:bold">Knight</span>                                                                                         |

### RGB Gradients Made Easy

Use [birdflop.com/resources/rgb](https://www.birdflop.com/resources/rgb/) to generate gradient text:

1. Type your name on the site
2. Set output format to **MiniMessage**
3. Copy the output and paste it into the nickname dialog

> See the [MiniMessage docs](https://docs.papermc.io/adventure/minimessage/format/) for the full formatting syntax.

## Notes

- Nicknames are persistent across sessions
- The nickname shows in the tab list
- All players can use `/nick` by default
