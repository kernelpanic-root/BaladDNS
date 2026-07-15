// IPrivilegedService.aidl
package com.kernelpanic.baladdns;

// Declare any non-default types here with import statements

interface IPrivilegedService {
    boolean grantWriteSecureSettings(String packageName);
}