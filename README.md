# Gestion des Comptes 

Petite application Android + backend Spring Boot pour gérer des comptes (CRUD). Contient un client Retrofit (JSON/XML) et une UI RecyclerView.

## Structure
- app/ : module Android (UI, Retrofit client, adapter)
- server/ : (séparé) Spring Boot backend qui expose `/banque/comptes`
- src/... : code Java/Kotlin du projet Android

## Prérequis
- Java 11+
- Android SDK (platform-tools pour adb)
- Gradle
- Emulator (LDPlayer) ou appareil physique
- Spring Boot (server) lancé localement sur le port 8082
- 
## Démonstration

https://github.com/user-attachments/assets/2d2fb646-e47f-4806-9af7-4303f66107d0

## Configuration importante (Android)
- Retrofit base URL :
  `\app\src\main\java\ma\projet\restclient\config\RetrofitClient.java`
    - En dev avec LDPlayer ou retour réseau : `BASE_URL = "http://192.168.1.107:8082/";` (remplacez par l'IP de votre PC)
    - Ou utilisez `adb reverse tcp:8082 tcp:8082` puis `BASE_URL = "http://127.0.0.1:8082/";`

- Autoriser Internet (AndroidManifest.xml) :
  ```xml
  <uses-permission android:name="android.permission.INTERNET" />
  ```

- Si HTTP (non TLS) bloque par la politique réseau, ajouter `res/xml/network_security_config.xml` et référencer dans `<application android:networkSecurityConfig="@xml/network_security_config">` ou pour debug `android:usesCleartextTraffic="true"`.

## Démarrer le backend (Spring Boot)
1. Depuis le dossier du server :
   ```
   ./mvnw spring-boot:run
   ```
   Vérifier : http://localhost:8082/banque/comptes

2. Si vous voulez que l'émulateur y accède, vérifiez que l'app écoute sur `0.0.0.0` (ou utilisez l'IP du PC).

## Lancer l'application Android
1. Ouvrir le projet dans Android Studio ou via Gradle:
   ```
   ./gradlew assembleDebug
   ```
2. Installer sur l'émulateur / LDPlayer ou appareil.
3. Vérifier la page comptes dans l'app — si vide, activer le logging Retrofit (HttpLoggingInterceptor) pour debug.

## Commandes Git basiques pour pousser le repo
```
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/<votre-utilisateur>/<repo>.git
git push -u origin main
```

## Débogage rapide
- Test endpoint depuis l'émulateur/navigateur : `http://192.168.1.107:8082/banque/comptes`
- Si timeout/connection refused :
    - vérifier `netstat -ano | findstr :8082` sur la machine hôte
    - autoriser le port 8082 dans le pare-feu ou utiliser `adb reverse`
- Activer HttpLoggingInterceptor pour voir requêtes/réponses
