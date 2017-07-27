import { IonicNativePlugin } from '@ionic-native/core';
import { Observable } from 'rxjs/Observable';
/**
 * @name Serial
 * @description
 * This plugin provides functions for working with Serial connections
 *
 * @usage
 * ```typescript
 * import { Serial } from '@ionic-native/serial';
 *
 * constructor(private serial: Serial) { }
 *
 * ...
 *
 * this.serial.requestPermission().then(() => {
 *   this.serial.open({
 *     baudRate: 9800
 *   }).then(() => {
 *     console.log('Serial connection opened');
 *   });
 * }).catch((error: any) => console.log(error));
 *
 * ```
 */
export declare class UsbHid extends IonicNativePlugin {
    enumerateDevices(): Promise<any>;
    requestPermission(device: any): Promise<any>;
    open(device: any): Promise<any>;
    stop(device: any): Promise<any>;
    registerReadCallback(): Observable<any>;
}
