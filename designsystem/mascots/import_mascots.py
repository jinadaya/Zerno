#!/usr/bin/env python3
"""Writes MascotVectors.kt from the mascot SVGs in this folder.

Usage: python3 designsystem/mascots/import_mascots.py

The SVGs come from the project's mascot/ folder (with their provenance metadata removed). They use paths, circles,
ellipses and rects with fill, stroke, opacity and translate / rotate / scale transforms; anything else fails loudly
here instead of rendering wrong. Do not edit MascotVectors.kt by hand.
"""
import os
import re
import xml.etree.ElementTree as ET

CHARACTERS = [("Woolly", "woolly"), ("Lumi", "lumi"), ("Pip", "pip")]
POSES = [("Welcome", "welcome"), ("Reading", "reading"), ("Streak", "streak"), ("WelcomeBack", "welcome-back"), ("Achievement", "achievement")]
NS = "{http://www.w3.org/2000/svg}"


def f(v):
    x = float(v)
    s = repr(x)
    if s.endswith(".0"):
        s = s[:-2] + ".0"
    return s + "f"


def dedupe_attributes(text):
    """Some tags repeat an attribute (stroke-width twice). Keep the last one, which is the intended override."""
    def fix(match):
        tag = match.group(0)
        attrs = re.findall(r'\s([a-zA-Z:-]+)="([^"]*)"', tag)
        head = re.match(r"<\s*[a-zA-Z]+", tag).group(0)
        tail = "/>" if tag.rstrip().endswith("/>") else ">"
        merged = {}
        for k, v in attrs:
            merged[k] = v
        return head + "".join(f' {k}="{v}"' for k, v in merged.items()) + tail
    return re.sub(r"<[a-zA-Z][^>]*>", fix, text)


def shape_path(tag, a):
    if tag == "path":
        return a["d"]
    if tag == "circle":
        cx, cy, r = float(a["cx"]), float(a["cy"]), float(a["r"])
        return f"M{cx - r} {cy}a{r} {r} 0 1 0 {2 * r} 0a{r} {r} 0 1 0 {-2 * r} 0Z"
    if tag == "ellipse":
        cx, cy, rx, ry = float(a["cx"]), float(a["cy"]), float(a["rx"]), float(a["ry"])
        return f"M{cx - rx} {cy}a{rx} {ry} 0 1 0 {2 * rx} 0a{rx} {ry} 0 1 0 {-2 * rx} 0Z"
    if tag == "rect":
        x, y = float(a.get("x", 0)), float(a.get("y", 0))
        w, h = float(a["width"]), float(a["height"])
        rx = float(a.get("rx", a.get("ry", 0)))
        ry = float(a.get("ry", rx))
        if rx == 0:
            return f"M{x} {y}h{w}v{h}h{-w}Z"
        return (f"M{x + rx} {y}h{w - 2 * rx}a{rx} {ry} 0 0 1 {rx} {ry}v{h - 2 * ry}a{rx} {ry} 0 0 1 {-rx} {ry}"
                f"h{-(w - 2 * rx)}a{rx} {ry} 0 0 1 {-rx} {-ry}v{-(h - 2 * ry)}a{rx} {ry} 0 0 1 {rx} {-ry}Z")
    raise ValueError(f"unsupported element <{tag}>")


def transforms(value):
    """SVG transform list to a list of single operations, outermost first."""
    ops = []
    for name, args in re.findall(r"(translate|rotate|scale)\(([^)]*)\)", value or ""):
        nums = [float(n) for n in re.split(r"[\s,]+", args.strip()) if n]
        ops.append((name, nums))
    rest = re.sub(r"(translate|rotate|scale)\(([^)]*)\)", "", value or "").strip()
    if rest:
        raise ValueError(f"unsupported transform {value}")
    return ops


def group_open(op):
    name, n = op
    if name == "translate":
        return f"group(translationX = {f(n[0])}, translationY = {f(n[1] if len(n) > 1 else 0)}) {{"
    if name == "rotate":
        if len(n) == 3:
            return f"group(rotate = {f(n[0])}, pivotX = {f(n[1])}, pivotY = {f(n[2])}) {{"
        return f"group(rotate = {f(n[0])}) {{"
    sx = n[0]
    sy = n[1] if len(n) > 1 else n[0]
    return f"group(scaleX = {f(sx)}, scaleY = {f(sy)}) {{"


def color(v):
    if v is None or v == "none":
        return None
    m = re.fullmatch(r"#([0-9a-fA-F]{6})", v)
    if not m:
        raise ValueError(f"unsupported colour {v}")
    return f"Color(0xFF{m.group(1).upper()})"


CAPS = {"round": "StrokeCap.Round", "square": "StrokeCap.Square", "butt": "StrokeCap.Butt"}
JOINS = {"round": "StrokeJoin.Round", "bevel": "StrokeJoin.Bevel", "miter": "StrokeJoin.Miter"}


def emit(el, indent, out):
    tag = el.tag.replace(NS, "")
    pad = "    " * indent
    ops = transforms(el.attrib.get("transform"))
    for op in ops:
        out.append(pad + group_open(op))
        indent += 1
        pad = "    " * indent
    if tag == "g":
        for child in el:
            emit(child, indent, out)
    else:
        a = el.attrib
        fill = color(a.get("fill", "#000000"))
        stroke = color(a.get("stroke"))
        opacity = float(a.get("opacity", 1))
        args = []
        if fill:
            args.append(f"fill = SolidColor({fill})")
            if opacity != 1:
                args.append(f"fillAlpha = {f(opacity)}")
        if stroke:
            args.append(f"stroke = SolidColor({stroke})")
            args.append(f"strokeLineWidth = {f(a.get('stroke-width', 1))}")
            args.append(f"strokeLineCap = {CAPS[a.get('stroke-linecap', 'butt')]}")
            args.append(f"strokeLineJoin = {JOINS[a.get('stroke-linejoin', 'miter')]}")
            if opacity != 1:
                args.append(f"strokeAlpha = {f(opacity)}")
        d = shape_path(tag, a)
        out.append(pad + f'addPath(addPathNodes("{d}"), {", ".join(args)})')
    for _ in ops:
        indent -= 1
        out.append("    " * indent + "}")


def vector(svg_file, name):
    text = dedupe_attributes(open(svg_file).read())
    root = ET.fromstring(text)
    assert root.attrib.get("viewBox") == "0 0 200 200", svg_file
    out = []
    for child in root:
        emit(child, 3, out)
    body = "\n".join(out)
    return (f'ImageVector.Builder(name = "{name}", defaultWidth = 200.dp, defaultHeight = 200.dp, '
            f"viewportWidth = 200f, viewportHeight = 200f).apply {{\n{body}\n        }}.build()")


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    entries = []
    for cname, cdir in CHARACTERS:
        for pname, pfile in POSES:
            v = vector(os.path.join(here, cdir, f"{pfile}.svg"), f"{cname}{pname}")
            entries.append(f"    val {cname}{pname}: ImageVector by lazy {{\n        {v}\n    }}\n")
    out = f"""// Generated by designsystem/mascots/import_mascots.py from designsystem/mascots/*/*.svg. Do not edit.
package app.zerno.designsystem.mascot

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

/** The 15 mascot drawings, 200 x 200, in the fixed ink and sticker colours they were drawn in. */
internal object MascotVectors {{
{chr(10).join(entries)}}}
"""
    target = os.path.join(here, "..", "src", "commonMain", "kotlin", "app", "zerno", "designsystem", "mascot", "MascotVectors.kt")
    os.makedirs(os.path.dirname(target), exist_ok=True)
    with open(target, "w") as fh:
        fh.write(out)


if __name__ == "__main__":
    main()
