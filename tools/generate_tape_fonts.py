#!/usr/bin/env python3
"""Build iDea Tape font subsets with lv_font_conv 1.5.3 and Noto Sans CJK SC (OFL)."""
import argparse
from pathlib import Path
import re
import subprocess
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--font',required=True)
p.add_argument('--converter',required=True)
a=p.parse_args()
root=Path(__file__).resolve().parents[1]
chars=set(chr(i) for i in range(32,127))
for file in list((root/'main').glob('tape_*.[ch]'))+[root/'main/idea_main.c']:
    for literal in re.findall(r'"([^"\n]*)"',file.read_text()):
        chars.update(c for c in literal if ord(c)>=32)
symbols=''.join(sorted(chars))
(root/'assets/fonts/tape-characters.txt').write_text(symbols+'\n')
for size in (12,16,30):
    subprocess.run([a.converter,'--font',a.font,'--symbols',symbols if size!=30 else '0123456789:',
        '--size',str(size),'--bpp','2','--format','lvgl','--no-compress',
        '--lv-font-name',f'tape_font_{size}','--lv-include','lvgl.h',
        '--output',str(root/f'assets/fonts/tape_font_{size}.c')],check=True)
