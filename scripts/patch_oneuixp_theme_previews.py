#!/usr/bin/env python3
"""One-off, fail-closed layout update for the two OneuiXP theme mini-previews.

Never changes real pages, colors, theme settings, or SystemUI injection code.
"""
from pathlib import Path

root = Path('app/src/main/java/me/weishu/kernelsu/ui/screen/colorpalette')

miuix = root / 'ColorPaletteScreenMiuix.kt'
source = miuix.read_text(encoding='utf-8')
start_marker = '''                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp)'''
end_marker = '''
                }
            }

            if (useRail)'''
assert source.count(start_marker) == 1, 'Miuix preview start not unique'
a = source.index(start_marker)
b = source.index(end_marker, a)
replacement = '''                    // Mirror the live OneuiXP dashboard: one tall left card and
                    // two small right cards. The information card below stays intact.
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(68.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentCardColor)
                            )
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                repeat(2) {
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(cardColor)
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(cardColor)
                        )
                        Box(
                            modifier = Modifier.height(12.dp).fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(cardColor)
                        )
                        Box(
                            modifier = Modifier.height(12.dp).fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(cardColor)
                        )
                    }'''
updated = source[:a] + replacement + source[b:]
assert updated.count('Mirror the live OneuiXP dashboard') == 1
miuix.write_text(updated, encoding='utf-8')

material = root / 'ColorPaletteScreenMaterial.kt'
source = material.read_text(encoding='utf-8')
start_marker = '                    BoxWithConstraints(modifier = Modifier.weight(1f)) {'
end_marker = '''
            }

            if (useRail)'''
assert source.count(start_marker) == 1, 'Material preview start not unique'
a = source.index(start_marker)
b = source.index(end_marker, a)
replacement = '''                    // The miniature Material preview follows the same 3-card
                    // geometry as the actual OneuiXP home page.
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(62.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            TonalCard(
                                containerColor = colorScheme.secondaryContainer,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                shape = RoundedCornerShape(8.dp),
                                content = { },
                            )
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                repeat(2) {
                                    TonalCard(
                                        containerColor = colorScheme.surfaceBright,
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        content = { },
                                    )
                                }
                            }
                        }
                        TonalCard(
                            containerColor = colorScheme.surfaceBright,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            content = { },
                        )
                    }'''
updated = source[:a] + replacement + source[b:]
assert updated.count('miniature Material preview') == 1
material.write_text(updated, encoding='utf-8')

print('PASS: Miuix and Material previews now show left large + two right small cards')
