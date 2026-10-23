#!/usr/bin/env python3
"""Verify that the open Android APK contains no proprietary map SDK or native libraries."""
import argparse,zipfile
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('apk',type=Path)
a=p.parse_args()
with zipfile.ZipFile(a.apk) as apk:
    assert apk.testzip() is None, 'Damaged APK'
    assert 'classes.dex' in apk.namelist(), 'DEX missing'
    assert not any(n.endswith('.so') for n in apk.namelist()), 'Unexpected native library'
    for name in apk.namelist():
        assert not name.startswith('assets/ae/'), 'Unexpected vendor map assets'
        if name.endswith('.dex'):
            data=apk.read(name)
            assert b'com/amap/' not in data and b'com/baidu/mapapi/' not in data, 'Unexpected vendor map SDK'
print('APK integrity and map-SDK-free package: PASS')
