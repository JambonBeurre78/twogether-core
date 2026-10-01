# Tools

## Models in Blockbench

Every block model is a plain Java model under `src/main/resources/assets/twogethercore/models/block/`.
Blockbench opens them directly (File > Open Model) and finds the textures through the `assets` folder.
Edit, then File > Export > Block/Item Model over the same file.

Bottles and glasses come in many variants (8 spirits x 4 sips, 17 drinks), so they are not edited one by
one: edit the template, then regenerate.

| Template | Variants built from it |
|---|---|
| `models/block/template/spirit_bottle.json` | `spirit_bottle_<spirit>_<sips>.json` + blockstate |
| `models/block/template/stemmed_glass.json` | `stemmed_glass_<drink>.json` + blockstate |

```
python tools/drink_models.py
```

Keep the element named `liquid`: in the bottle template its height is the full level (the script lowers it
for fewer sips), and the script removes it for the empty glass.
