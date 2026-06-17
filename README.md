# MyAndroidApp

Aplikacja mobilna napisana w Kotlinie w Android Studio. Projekt służy do zapisywania i analizowania inwestycji oraz pozycji, z widokiem podsumowania, wykresów, dokumentów i profilu użytkownika.

## Technologie

- Kotlin
- Android Studio
- Gradle Kotlin DSL
- AndroidX
- Material Components
- Retrofit
- OkHttp
- Coroutines
- MPAndroidChart

## Funkcje

- logowanie użytkownika,
- widok podsumowania inwestycji,
- statystyki wejścia, zamknięcia, zysku i procentu zwrotu,
- statystyki miesięczne,
- obsługa wykresów,
- obsługa dokumentów i profilu,
- integracje sieciowe przygotowane pod zewnętrzne API.

## Uruchomienie projektu

1. Pobierz repozytorium lub sklonuj je z GitHuba.
2. Otwórz projekt w Android Studio.
3. Poczekaj na synchronizację Gradle.
4. Wybierz emulator lub podłączony telefon z włączonym debugowaniem USB.
5. Kliknij **Run**.

## Informacja o plikach Google

Projekt nie wymaga już pliku `google-services.json` do samego zbudowania aplikacji, ponieważ usunięto plugin `com.google.gms.google-services` z konfiguracji Gradle.

Pliki takie jak `google-services.json` oraz `client_secret*.json` są wpisane do `.gitignore` i nie powinny być wrzucane do repozytorium, ponieważ mogą zawierać dane konfiguracyjne lub prywatne.

## Autor

BrunonToJa
