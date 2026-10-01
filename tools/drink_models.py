# Builds every spirit bottle and stemmed glass block model from two templates.
#
#   python tools/drink_models.py
#
# The templates are ordinary Java block models, made to be edited in Blockbench:
#   src/main/resources/assets/twogethercore/models/block/template/spirit_bottle.json
#   src/main/resources/assets/twogethercore/models/block/template/stemmed_glass.json
# Open one in Blockbench (File > Open Model; textures are found through the assets folder), change
# the shape, then File > Export > Block/Item Model over the same file and run this script.
# The only rule: keep the element named "liquid". Its height in the bottle template is the FULL
# level; this script lowers it for 3, 2 and 1 sips left, and drops it from the empty glass.
# ASCII only.
import copy
import io
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "twogethercore")
MODELS = os.path.join(ASSETS, "models", "block")
TEMPLATES = os.path.join(MODELS, "template")

MAX_SIPS = 4
# Same order as Drink.java.
DRINKS = ["empty", "whisky", "aged_whisky", "brandy", "aged_brandy", "calvados", "vodka", "rum", "dark_rum",
          "old_fashioned", "hot_toddy", "berry_daiquiri", "rum_punch", "black_russian", "brandy_alexander",
          "jack_rose", "ender_martini"]
SPIRITS = DRINKS[1:9]


def load(name):
    with io.open(os.path.join(TEMPLATES, name + ".json"), encoding="utf-8") as f:
        model = json.load(f)
    model.pop("credit", None)
    return model


def save(path, data):
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def liquid(model):
    found = [e for e in model["elements"] if e.get("name") == "liquid"]
    if len(found) != 1:
        raise SystemExit("the template needs exactly one element named 'liquid'")
    return found[0]


bottle = load("spirit_bottle")
bottom, full_top = liquid(bottle)["from"][1], liquid(bottle)["to"][1]
for spirit in SPIRITS:
    for sips in range(1, MAX_SIPS + 1):
        model = copy.deepcopy(bottle)
        model["textures"]["liquid"] = "twogethercore:block/drink_" + spirit
        liquid(model)["to"][1] = round(bottom + (full_top - bottom) * sips / MAX_SIPS, 3)
        save(os.path.join(MODELS, "spirit_bottle_%s_%d.json" % (spirit, sips)), model)
save(os.path.join(ASSETS, "blockstates", "spirit_bottle.json"), {"variants": {
    "sips=%d,spirit=%s" % (s, n): {"model": "twogethercore:block/spirit_bottle_%s_%d" % (n, s)}
    for n in SPIRITS for s in range(1, MAX_SIPS + 1)}})

glass = load("stemmed_glass")
for drink in DRINKS:
    model = copy.deepcopy(glass)
    if drink == "empty":
        model["elements"] = [e for e in model["elements"] if e.get("name") != "liquid"]
        model["textures"].pop("liquid", None)
    else:
        model["textures"]["liquid"] = "twogethercore:block/drink_" + drink
    save(os.path.join(MODELS, "stemmed_glass_%s.json" % drink), model)
save(os.path.join(ASSETS, "blockstates", "stemmed_glass.json"), {"variants": {
    "drink=" + d: {"model": "twogethercore:block/stemmed_glass_" + d} for d in DRINKS}})

print("%d bottle and %d glass models written" % (len(SPIRITS) * MAX_SIPS, len(DRINKS)))
