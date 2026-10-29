#!/usr/bin/env python3
"""Integrity/coverage gates for the pinned original and actual runtime snapshot."""
import argparse
import collections
import hashlib
import json
import pathlib
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]


def verify(legacy_jar):
    r = json.loads((ROOT / 'reference/content-inventory.json').read_text())
    snapshot = r['runtime_snapshot']
    expected = {'aspect': 37, 'block': 200, 'block_entity_type': 48, 'entity_type': 43, 'fluid': 3,
                'focus': 21, 'golem_addon': 4, 'golem_arm': 5, 'golem_head': 5, 'golem_leg': 4,
                'golem_material': 6, 'golem_trait': 19, 'item': 108, 'mob_effect': 9,
                'recipe_call': 302, 'research': 148, 'seal': 16, 'sound_event': 65,
                'theory_aid': 14, 'theory_card': 33, 'worldgen/biome': 3}
    # Pinned BETA26 sentinels: changing the source release requires explicit review.
    for kind, count in expected.items(): assert r['counts'][kind] == count, (kind, r['counts'].get(kind))
    assert not r['unresolved_identities']
    assert not snapshot['gameplay_verified'] and not snapshot['client_rendered']
    assert all(e['port']['gameplay_parity'] == 'not_verified' and e['port']['visual_parity'] == 'not_verified' for e in r['entries'])
    index = {(e['kind'], e['id']): e for e in r['entries']}
    assert len(index) == len(r['entries'])
    assert ('item', 'thaumcraft:caster_advanced') not in index  # texture/class != registration
    assert ('theory_card', 'CardTruth') not in index and ('theory_card', 'CardDragonEgg') not in index
    assert len(index['item', 'thaumcraft:curio']['metadata_variants']) == 7
    assert len(index['item', 'thaumcraft:celestial_notes']['metadata_variants']) == 13
    assert len(index['item', 'thaumcraft:nugget']['metadata_variants']) == 11
    assert len(index['item', 'thaumcraft:chunk']['metadata_variants']) == 6
    for family in ('banner', 'candle', 'nitor'):
        assert ('block', f'thaumcraft:{family}_lightblue') in index
        assert ('block', f'thaumcraft:{family}_light_blue') not in index
    with zipfile.ZipFile(legacy_jar) as z:
        assert hashlib.sha256(pathlib.Path(legacy_jar).read_bytes()).hexdigest() == r['legacy_sha256']
        for entry in r['entries']:
            if entry['kind'] == 'block': assert 'assets/thaumcraft/blockstates/' + entry['id'].split(':')[1] + '.json' in z.namelist()
        for media in r['media']:
            assert hashlib.sha256(z.read(media['source'])).hexdigest() == media['sha256']
            for port_file in media['byte_identical_port_files']:
                assert hashlib.sha256((ROOT / port_file).read_bytes()).hexdigest() == media['sha256']
    ids = set(snapshot['registries']['minecraft:item'])
    hidden = {k for k, v in snapshot['items'].items() if v['display_only']}
    assert len(hidden) == 157 and ids == set(snapshot['items'])
    for entry in r['entries']:
        if entry['kind'] == 'item': assert not hidden.intersection(entry['port']['registered_matches'])
    aliases = json.loads((ROOT / 'reference/parity-aliases.json').read_text())
    for kind, mapping in aliases.items():
        for source, targets in mapping.items():
            assert (kind, source) in index, (kind, source)
            for target in targets: assert target in snapshot['registries']['minecraft:' + kind], target
    assert any(any('for ' in c or c.startswith('for(') for c in e.get('contexts', [])) for e in r['entries'] if e['kind'] == 'recipe_call')
    assert any(any('if ' in c or c.startswith('if(') for c in e.get('contexts', [])) for e in r['entries'] if e['kind'] == 'recipe_call')
    blocked = [q for q in snapshot['item_requirements'] if not q['supported']]
    assert all(q['craft_key'].startswith('unsupported:') == (not q['supported']) for q in snapshot['item_requirements'])
    result = {'original_entries': len(index), 'original_counts': r['counts'], 'original_media': len(r['media']),
              'byte_identical_media': sum(bool(m['byte_identical_port_files']) for m in r['media']),
              'runtime_items': len(ids), 'display_only_items': len(hidden), 'non_display_items': len(ids - hidden),
              'item_requirements': len(snapshot['item_requirements']), 'unsupported_item_requirements': len(blocked),
              'research_with_unsupported_items': len({q['research'] for q in blocked}),
              'gameplay_parity_verified': False, 'client_started': False}
    return r, result


def markdown(r, result):
    lines = ['# Реестр переноса TC6: alpha.9.8', '',
             'Источник — Thaumcraft 6.1.BETA26. Это учёт объектов и обнаруженных пропусков, не процент готовности.', '',
             '**Ни одна строка не объявлена совпадающей 1 в 1 в игре.** Зарегистрированный предмет может не иметь рецепта, полной механики или источника в выживании.', '',
             'Столбец «Сопоставлено» означает наличие ID в реальном datagen-снимке либо определения в каталоге. Для теорий это не доступность карты/помощника.', '',
             '| Категория | Исходных записей | Сопоставлено |', '|---|---:|---:|']
    for kind, count in r['counts'].items():
        matches = sum(bool(e['port']['registered_matches']) for e in r['entries'] if e['kind'] == kind)
        lines.append(f'| {kind} | {count} | {matches} |')
    lines += ['', '## Границы подсчёта', '',
              '- 200 блоков включают 48 цветных блоков, служебные блоки и отдельные двойные плиты 1.12. Их предметы не повторяются среди 108 самостоятельных предметов.',
              '- 302 recipe_call — места регистрации с сохранёнными условиями и циклами, а не число рецептов. Отдельно требуется сверить развёрнутые и динамические рецепты.',
              '- В порте 292 ID предметов: 157 служебных значков и 135 остальных. Это не 292 перенесённых игровых предмета.',
              '- Учитываются все три зарегистрированных биома TC6. Регистрация Eldritch не доказывает существование доступного генератора отдельного измерения.',
              f'- Медиа: {result["original_media"]} исходных файлов; {result["byte_identical_media"]} найдены побайтно. Преобразованные JPG/MTL требуют своих проверок; отсутствие одинаковых байтов не равно отсутствию визуала.',
              '- Строки вариантов с component_behavior_audit_required требуют проверки компонентов/NBT. Простой совпавший ID не доказывает правильность варианта.',
              '- Все публичные API, события, частицы и мировые алгоритмы остаются предметом проверки по сценариям; очереди исходников сохранены в JSON.',
              '', '## Предметные требования, которые сейчас блокируются', '',
              '| Исследование | Этап | Условие | Исходный предмет |', '|---|---:|---|---|']
    for q in r['runtime_snapshot']['item_requirements']:
        if not q['supported']: lines.append(f'| {q["research"]} | {q["stage"]} | {q["field"]} | `{q["source"]}` |')
    lines += ['', 'Поддержка остальных предметных требований не проверяет все специальные условия, знания, награды или способы получения.', '',
              '## Пообъектное сопоставление', '', '| Тип | Оригинал | Найдено в порте | Источник |', '|---|---|---|---|']
    for e in r['entries']:
        if e['kind'] == 'recipe_call': continue
        s = e['source']; source = s.get('jar_path', s.get('path', '') + ':' + str(s.get('line', '')))
        lines.append(f'| {e["kind"]} | `{e["id"]}` | ' + ', '.join('`' + x + '`' for x in e['port']['registered_matches']) + f' | `{source}` |')
    lines += ['', 'Варианты metadata, выражения всех регистраций рецептов, хеши исходников и полный список медиа находятся в `reference/content-inventory.json`.', '']
    return '\n'.join(lines)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(); parser.add_argument('--legacy-jar', required=True); parser.add_argument('--report', type=pathlib.Path)
    args = parser.parse_args(); report, result = verify(args.legacy_jar)
    (ROOT / 'PARITY-STATUS.ru.md').write_text(markdown(report, result))
    if args.report: args.report.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(result, ensure_ascii=False, indent=2))
