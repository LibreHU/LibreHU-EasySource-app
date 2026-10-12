# LibreHU EasySource

Changer de source d'un appui au volant, sur autoradios Jancar / Autochips AC8257 (UJC201) : radio FM, musique
Bluetooth, entrée AUX, applis de musique… Thème clair / sombre et couleur d'accent de
[LibreHU Launcher](https://github.com/LibreHU/LibreHU-Launcher-App).

- **Sélecteur** plein écran par-dessus l'appli en cours, grandes tuiles : chaque appui sur **MODE / SRC** affiche la
  source suivante, qui démarre après quelques secondes sans touche (réglable) ou tout de suite avec OK / lecture ;
  ▶▶ / ◀◀ parcourent aussi la liste ; au toucher, une tuile démarre aussitôt.
- **Sources** : LibreHU FM, Bluetooth (A2DP de `com.android.bluetooth`), **entrée AUX** (source audio
  « aux » de [LibreHU-service](https://github.com/LibreHU/LibreHU-service)), et les applis média trouvées comme le
  fait Android Auto (`MediaBrowserService`) ou n'importe quelle appli ; ordre, activation et ajout dans l'appli.
- **Changement** : les autres sessions média sont mises en pause, l'entrée de la puce audio repasse sur Android (ou
  AUX), puis la source démarre par sa session média, en arrière-plan, ou en ouvrant l'appli (option). L'accès aux
  notifications est nécessaire pour voir les sessions des autres applis.
- **Reprise au démarrage** de la dernière source (option).

## Sur une touche du volant

Avec [LibreHU BtnRemap](https://github.com/LibreHU/LibreHU-BtnRemap-app) : touche MODE / SRC → « Ouvrir une appli »
→ **EasySource (sélecteur de source)**. D'autres applis peuvent démarrer le sélecteur :

```
am start -a org.librehu.easysource.action.NEXT        # source suivante
am start -a org.librehu.easysource.action.PREVIOUS    # source précédente
```

Non testé sur l'autoradio à ce stade.
