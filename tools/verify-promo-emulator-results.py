"""Fail closed if the instrumentation suite did not execute all three promo tests."""
from pathlib import Path
import xml.etree.ElementTree as ET

expected = {'rendersNativePromoThroughRealSdk', 'hidesNativePromoForEmptyMessaging',
            'handlesApiErrorWithoutCrashingSampleApp'}
seen = set()
for path in Path('samples-java/build/outputs/androidTest-results').rglob('*.xml'):
    for case in ET.parse(path).getroot().iter('testcase'):
        if case.get('classname') != 'com.affirm.android.PromoMessagingEmulatorTest':
            continue
        if any(case.find(kind) is not None for kind in ('failure', 'error', 'skipped')):
            raise SystemExit('Promo emulator test failed or skipped: ' + str(case.get('name')))
        seen.add(case.get('name'))
if seen != expected:
    raise SystemExit('Expected all three promo emulator tests; got ' + str(sorted(seen)))
print('Verified all three promo tests executed and passed on the emulator')
