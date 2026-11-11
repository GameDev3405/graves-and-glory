"""Shared release identity for packaging and validation; no launcher dependency."""
import hashlib
import pathlib
import re
import json
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]

def version():
    matches = re.findall(r'^mod_version=(\d+\.\d+\.\d+(?:-[A-Za-z0-9.-]+)?)$',
                         (ROOT / 'gradle.properties').read_text(), re.MULTILINE)
    if len(matches) != 1:
        raise ValueError('Expected one mod_version in gradle.properties')
    return matches[0]

def source_identity():
    paths = list((ROOT / 'src/main').rglob('*')) + [
        ROOT / n for n in ['build.gradle', 'gradle.properties', 'settings.gradle',
                            'sheets/design.json', 'tools/package.py',
                            'tools/release_config.py', 'tools/build_identity.py']]
    digest = hashlib.sha256()
    for p in sorted((p for p in paths if p.is_file()), key=lambda p: p.relative_to(ROOT).as_posix()):
        data = p.read_bytes()
        if p.suffix in {'.java', '.json', '.gradle', '.properties', '.py', '.mcmeta'}:
            data = data.replace(b'\r\n', b'\n')
        digest.update(p.relative_to(ROOT).as_posix().encode() + b'\0')
        digest.update(hashlib.sha256(data).digest())
    return {'version': version(), 'modId': 'gravesandglory', 'sourceSha256': digest.hexdigest()}

def validate_player_jar(path, expected):
    with zipfile.ZipFile(path) as jar:
        metadata = json.loads(jar.read('fabric.mod.json'))
        if metadata['id'] != expected['modId'] or metadata['version'] != expected['version']:
            raise RuntimeError('JAR identity/version differs from configured release; rebuild first')
        if json.loads(jar.read('gravesandglory-build.json')) != expected:
            raise RuntimeError('JAR was built from different source; rebuild before packaging')
        forbidden = ('MeltyTest', 'CampaignChecks', 'CampaignDevControl', 'MapRouteChecks',
                     'Phase3Checks', 'StructureStudio', 'Walkability', 'gravesandglory_studio', 'p3_marker',
                     'VisualChecks', 'RevisionChecks', 'LocalProbe', 'LegacyDataMigrationCheck',
                     'CompatibilityChecks', 'ConnectOnStart', 'CompatibilityPolicyCheck', 'TitleRenameCheck', 'FrontMenuChecks', 'AdmissionPolicyCheck', 'TeleportRelocateCheck', 'CampRulesCheck', 'LobbyRulesCheck', 'LobbyChecks', 'UiReportChecks', 'BannerFieldChecks', 'CampRedesignChecks', 'CampNetworkChecks', 'PerfProbe', 'PerfClient', 'PerfIntegrated', 'SliceProbe', 'SliceClient', 'MapTour', 'Phase1Checks', 'BattleMathCheck', 'BotSimulation', 'ServerKitSmoke', 'ZombieReinforcementProbe', 'ProfileFixtureCheck', 'Phase2Checks', 'HousesCheck', 'ProfileMigrationCheck', 'GearCheck', 'GearLoadoutCheck', 'GearMigrationCheck', 'ForgeCheck', 'GearWireCheck', 'GearCommandsCheck', 'GearChecks', 'GearStatsCheck', 'GearEffectsCheck', 'RelicSlotsCheck', 'GearCombatChecks', 'GearViewCheck', 'GearScreenChecks', 'GearArtChecks', 'HouseArtChecks', 'WeaponLookChecks', 'GeoKnightChecks', 'GeoGaps', 'GeoPerfChecks', 'PropLookChecks', 'PropGameplayChecks', 'PersistedDataCorruptionCheck', 'MatchRulesCheck', 'ArenaAutosaveCheck', 'MarchMathCheck', 'CampaignResetCheck', 'UiMathCheck', 'UiKitSheet', 'LevelMathCheck', 'LevelChecks', 'AbilityLevelChecks', 'RankChecks', 'RankNetworkChecks', 'SelfTestHost', 'CommandPlaytest', 'TitleDemo',
                     'CampTerrainCheck', 'HouseRouteCheck', 'StepHopCheck', 'EngineMapCheck', 'WedgeCheck', 'LayoutMarkerCheck', 'TroopFacesCheck', 'PropDefCheck', 'PropGeometryCheck', 'PropEffectsCheck', 'PropPackFileCheck', 'HandbookTextCheck', 'BannerDropCheck', 'AudioCuesCheck', 'FacesChecks',
                     'MotionChecks', 'MotionGrade', 'SoleSlide', 'MotionMathCheck', 'MotionEngineCheck', 'MotionStanceCheck', 'MotionReactCheck', 'MotionFrameCheck', 'MotionLifeCheck', 'MotionClothCheck', 'MotionLayersCheck', 'StepWindows', 'HoldLineChecks', 'StalkerReelChecks',
                     'DuelSimulation', 'ScratchFlags', 'SheetOverride', 'TypeLedger', 'StoryStills', 'ModeMathCheck', 'FeedFitCheck', 'ModeHudFitCheck', 'SurvivalFeedShortCheck', 'SiegePlayChecks', 'MapWalk', 'MapForge', 'mapforge-builder', 'adoption-dev/', 'ConvoyCheck', 'ConvoyChecks', 'MarchModesChecks', 'SurvivalChecks', 'MarchBonusChecks', 'Phase4Checks', 'CommandUiChecks', 'cmdui/', 'OrderIntakeProbe', 'FeedbackProbe', 'ChargeCameraProbe', 'CustomGroupsProbe', 'BarracksUiChecks', 'FeedbackMate', 'OrderRulesCheck', 'FormationRulesCheck', 'RewardDriver',
                     'AiPurposeOverlay', 'AiMetrics', 'AiState', 'gg/gravesandglory/testing/', 'META-INF/services/gg.gravesandglory.Diagnostics$Probe')
        for name in jar.namelist():
            if any(token in name for token in forbidden):
                raise RuntimeError('Development harness leaked into player JAR: ' + name)
        for category in ('main', 'client'):
            for entrypoint in metadata['entrypoints'][category]:
                if entrypoint.replace('.', '/') + '.class' not in jar.namelist():
                    raise RuntimeError('Missing entrypoint class: ' + entrypoint)
        return metadata
