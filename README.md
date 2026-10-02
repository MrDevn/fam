# FamilyAware

Нативное Android-приложение на **Kotlin + Jetpack Compose + Material 3**, ориентированное на **Android 16 (API 36)**.

Приложение показывает, как корректно работать на устройстве с родительским контролем (Google Family Link, рабочий профиль, MDM):
оно честно определяет ограничения, объясняет каждое разрешение и **не пытается ничего обойти**.

---

## Что приложение делает

- При первом запуске показывает мастер настройки из трёх шагов:
  1. что делает приложение;
  2. какие разрешения нужны и зачем, что будет при отказе;
  3. что происходит, если устройством управляет родитель.
- Диагностирует устройство официальными API и показывает результат.
- Управляет единственным runtime-разрешением (`POST_NOTIFICATIONS`) с полным набором состояний.
- Позволяет отправить тестовое уведомление и увидеть реальный результат.
- Показывает инструкцию для родителя, если функция заблокирована.

## Чего приложение НЕ делает

- не скрывает себя из списка приложений и не отключает свою иконку;
- не использует `AccessibilityService`, `Device Owner`, `Profile Owner`, `BIND_VPN_SERVICE`, overlay (`SYSTEM_ALERT_WINDOW`);
- не отключает и не обходит родительский контроль;
- не меняет системные настройки без явного действия пользователя;
- не запрашивает разрешения повторно автоматически — только по нажатию;
- не мешает удалению: удаление выполняется штатно через «Настройки» или магазин приложений;
- не объявляет `<queries>` и не читает список установленных приложений;
- не имеет разрешения `INTERNET` — данные не собираются и никуда не передаются.

---

## Разрешения

| Разрешение | Тип | Зачем |
|---|---|---|
| `android.permission.POST_NOTIFICATIONS` | runtime (API 33+) | Показать результат проверки и предупредить о блокировке со стороны родительского контроля |

Других разрешений у приложения нет.

---

## Диагностика родительского контроля

Используются только публичные API, не требующие привилегий:

| API | Что определяет |
|---|---|
| `UserManager.isManagedProfile()` | рабочий (управляемый) профиль |
| `UserManager.isProfile()` | частный профиль / private space |
| `UserManager.isSystemUser()` | запуск не в основном пользователе |
| `UserManager.isDemoUser()` | демо-режим устройства |
| `UserManager.isQuietModeEnabled()` | профиль приостановлен |
| `UserManager.hasUserRestriction(DISALLOW_*)` | активные ограничения пользователя |
| `RestrictionsManager.hasRestrictionsProvider()` | активен сервис управления приложениями (Family Link, MDM) |
| `RestrictionsManager.getApplicationRestrictions()` | ограничения, переданные приложению администратором |
| `PackageManager.isPermissionRevokedByPolicy()` | разрешение отозвано политикой устройства |
| `PackageManager.getApplicationEnabledSetting()` | приложение отключено администратором |
| `PackageManager.getInstallSourceInfo()` | источник установки (справочно) |

Каждый вызов обёрнут в `runCatching`: если система запрещает конкретную проверку,
она просто не попадает в отчёт, а приложение не падает.

---

## Режим постоянного доступа

Отдельный режим для устройства, которое родитель **официально** разрешил использовать
без ограничения экранного времени. Режим относится только к самому приложению:
оно перестаёт показывать собственные напоминания и держит постоянную отметку в уведомлениях.

### Как включается

| Способ | Механизм | Кто решает |
|---|---|---|
| Ограничение приложения | `persistentAccessAllowed` через `RestrictionsManager` / `ApplicationRestrictionsController` / MDM. Объявлено в `res/xml/app_restrictions.xml`, отдаётся системе через `AppRestrictionsReceiver` | родитель или администратор |
| Системный диалог согласования | `RestrictionsManager.createLocalApprovalIntent()` → запрос уходит в сервис родительского контроля, ответ возвращается в приложение | родитель |
| Подтверждение на устройстве | явный диалог в приложении, **всегда со сроком действия** (1/6/12/24 ч) | пользователь, только если сервиса родительского контроля нет |

Приоритет всегда у администратора: если `persistentAccessAllowed = false`, режим выключен
и локально не включается — кнопки активации не показываются вообще.

Изменение ограничений отслеживается через официальный broadcast
`Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED`, поэтому отзыв срабатывает сразу.

### Состояния

`DISABLED` → `PENDING_APPROVAL` → `ACTIVE` → `EXPIRED` / `REVOKED_BY_ADMIN` /
`UNAVAILABLE_NO_NOTIFICATIONS`.

Режим не активируется без разрешения на уведомления: активный режим обязан быть виден
пользователю и родителю, поэтому в уведомлениях всегда висит неснимаемая отметка,
которая исчезает сразу после отзыва.

### Чего режим НЕ делает

- не изменяет системные лимиты экранного времени и не может их изменить — это делает только родитель в Family Link;
- не назначает приложение Device Owner / Profile Owner и не использует `DevicePolicyManager`;
- не запускает foreground-сервис, wakelock или keep-alive, чтобы «пережить» ограничения;
- не блокирует удаление приложения и не отключает свои компоненты;
- не скрывает активный режим;
- не переопределяет запрет администратора.

---

## Состояния разрешений

| Состояние | Поведение UI |
|---|---|
| `GRANTED` | функция доступна, предлагается тест |
| `NOT_REQUESTED` | кнопка «Запросить разрешение» |
| `DENIED` | кнопка «Спросить ещё раз» — **только по нажатию** |
| `DENIED_PERMANENTLY` | кнопки запроса нет, ведут в системные настройки |
| `BLOCKED_BY_POLICY` | кнопки запроса нет, показывается инструкция для родителя |
| `NOT_APPLICABLE` | на Android 12 и ниже runtime-запроса не существует |

Все экраны используют единый контракт `UiState`: `Idle` / `Loading` / `Success` / `Error` / `PermissionDenied`.

---

## Архитектура

```
app/src/main/java/com/fam/aware/
├── MainActivity.kt              # edge-to-edge, тема, корень
├── FamApp.kt                    # Application + контейнер зависимостей
├── di/                          # AppContainer, FamViewModelFactory
├── data/
│   ├── local/                   # SettingsRepository (SharedPreferences)
│   ├── model/                   # доменные модели + чистая логика (SupervisionAnalyzer, PersistentAccessPolicy)
│   ├── notification/            # NotificationPublisher
│   └── repository/              # SupervisionRepository, PermissionRepository, PersistentAccessRepository
├── receiver/                    # AppRestrictionsReceiver (официальный провайдер ограничений)
├── viewmodel/                   # Onboarding, Home, Appearance, PersistentAccess
├── ui/
│   ├── theme/                   # Color, Type, Theme, Motion/Dimens
│   ├── components/              # переиспользуемые компоненты и панели состояний
│   ├── onboarding/              # мастер первичной настройки
│   ├── home/                    # главный экран
│   ├── persistent/              # экран режима постоянного доступа
│   └── AppRoot.kt               # переключение экранов
└── util/                        # системные Intent'ы, разбор ответа родителя, форматирование
```

- UI не знает про системные API — только про ViewModel.
- ViewModel не знает про `Context` — только про интерфейсы репозиториев.
- Логика диагностики (`SupervisionAnalyzer`) — чистая функция, покрыта JVM-тестами.
- Все публичные состояния — `StateFlow`, в UI собираются через `collectAsStateWithLifecycle()`.
- Сторонних библиотек сверх стандартного набора AndroidX нет: без Hilt, Room, Navigation и Retrofit.

---

## Android 16 (API 36)

- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.
- Версии AndroidX подобраны под `compileSdk 36`: Compose BOM `2026.06.01` (UI 1.11.4, Material 3 1.4.0),
  `core-ktx 1.18.0`, `lifecycle 2.10.0`, `activity-compose 1.13.0`. Более свежие релизы
  этих библиотек требуют `minCompileSdk = 37`, то есть уже Android 17.
- **Edge-to-edge** включён явно (`enableEdgeToEdge`), все экраны обрабатывают insets
  (`statusBarsPadding`, `navigationBarsPadding`, `imePadding`, `Scaffold`).
- **Predictive back** включён через `android:enableOnBackInvokedCallback="true"`;
  перехватов кнопки «Назад» в приложении нет, поэтому системная анимация работает штатно.
- **Ориентация и размер окна не фиксируются**: нет `android:screenOrientation`
  и `android:resizeableActivity="false"` — Android 16 всё равно игнорирует их на больших экранах.
- Адаптивная сетка контента: `GridCells.Adaptive` даёт одну колонку на телефоне
  и две на планшете, складном устройстве и в desktop-режиме.
- Тёмная тема постоянная, опционально включается динамическая палитра Material You (Android 12+).

---

## Сборка

Требуется JDK 21 и Android SDK с `platforms;android-36` и `build-tools;36.0.0`.

```bash
# укажите путь к SDK
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew :app:testDebugUnitTest   # модульные тесты
./gradlew :app:lintDebug           # статический анализ
./gradlew :app:assembleDebug       # отладочный APK
./gradlew :app:assembleRelease     # релизный APK
```

Результат: `app/build/outputs/apk/`.

### Подпись релиза

По умолчанию релиз собирается **неподписанным**. Чтобы включить подпись,
задайте переменные окружения (секреты в репозиторий не попадают):

| Переменная | Значение |
|---|---|
| `FAM_KEYSTORE_PATH` | путь к файлу keystore |
| `FAM_KEYSTORE_PASSWORD` | пароль keystore |
| `FAM_KEY_ALIAS` | алиас ключа |
| `FAM_KEY_PASSWORD` | пароль ключа |

---

## CI: GitHub Actions

Workflow `.github/workflows/build.yml` запускается на каждый push в `main`, на PR и вручную:

1. ставит JDK 21 и Android SDK (API 36, build-tools 36.0.0);
2. прогоняет модульные тесты;
3. запускает Android Lint;
4. собирает debug- и release-APK;
5. публикует артефакты (APK, отчёт lint, отчёт тестов).

Отдельный job `release` срабатывает на теги вида `v*` и публикует GitHub Release.
Если в репозитории заданы секреты `FAM_KEYSTORE_BASE64`, `FAM_KEYSTORE_PASSWORD`,
`FAM_KEY_ALIAS`, `FAM_KEY_PASSWORD` — APK подписывается, иначе публикуется неподписанным.

```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## Лицензия

Apache License 2.0.
