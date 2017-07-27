# Générer les types pour typescript

- Télécharger ionic-native et exécuter les commandes :
```bash
git clone https://github.com/ionic-team/ionic-native.git
gulp plugin:create -n UsbHid
```

- Copier le fichier ```types/index.d.ts``` de ce projet dans le 
dossier ```ionic-native/src/@ionic-native/plugins/usb-hid/```

- Dans le terminal, aller dans le dossier ```ionic-native``` et 
exécuter la commande :
```bash
npm run build
```

- Copier les fichiers ci-dessous dans le dossier src de votre 
projet (ou ailleurs)
```
index.d.ts
index.js
index.js.map
index.metadata.json
```

# Utiliser le plugin pour votre application

- Mettre ce plugin à côté de votre application.

- Dans votre application, exécutez :
```
ionic cordova plugin add /lien/vers/ionic-heartbeat-android-plugin
```

