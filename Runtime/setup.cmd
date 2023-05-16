@echo off
echo "Register file extension ..."
powershell -Command "(gc %~dp0lcf-File.reg) -replace '%%INSTALL_PATH%%', ('%~dp0' -replace '\\', '\\') | Out-File %~dp0lcf-File2.reg"
reg import %~dp0lcf-File2.reg
del %~dp0lcf-File2.reg
echo "Done"