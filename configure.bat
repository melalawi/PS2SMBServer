@echo off
for /f "tokens=3,*" %%a in ('netsh interface show interface ^| findstr /i "Enabled"') do (
    set ifname=%%b
    if "%%b"=="PS2Network" goto connect
    if "%%b"=="TP-Link Wireless USB Adapter" goto rename
)
echo No connected interface found, exiting...
goto :EOF

:connect
echo Found connected interface (%ifname%), connecting to PS2 WIFI
netsh wlan connect ssid=OnlyPS2 interface="PS2Network" name="OnlyPS2"
netsh interface ip set address "PS2Network" static 192.168.8.3
goto :EOF

:rename
echo Found connected interface (%ifname%), renaming...
netsh interface set interface name="TP-Link Wireless USB Adapter" newname="PS2Network"
goto :connect

pause