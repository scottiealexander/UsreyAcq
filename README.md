# UsreyAcq
Micro-manager plugin for experiment control via Spike2 over serial. The system diagram is approximatly:

```
      (Serial)
Spike2 ------> UsreyAcq Plugin ----> Micro-Manager ----> Camera -
|                                                                |
|                                                (BNC)           |
 ------------> Data acquisition (1401) <-------------------------
|
 ------------> Visual stimulus
```

For build instructions see:

[Writing plugins for Micro-Manager](https://micro-manager.org/wiki/Writing_plugins_for_Micro-Manager)

This plugin has ben built using Eclipse, Netbeans has not been tested.
