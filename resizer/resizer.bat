@REM AndroidDrawableResizer (https://github.com/delight-im/AndroidDrawableResizer)
@REM Copyright (c) delight.im (https://www.delight.im/)
@REM Licensed under the MIT License (https://opensource.org/licenses/MIT)

@echo off

@REM ### BEGIN CONFIGURATION ###
set IMAGE_MAGICK_PATH="C:\Libraries\ImageMagick\convert.exe"
set PATH_LDPI=drawable-ldpi
set PATH_MDPI=drawable-mdpi
set PATH_HDPI=drawable-hdpi
set PATH_XHDPI=drawable-xhdpi
set PATH_XXHDPI=drawable-xxhdpi
set PATH_XXXHDPI=drawable-xxxhdpi
@REM ### END CONFIGURATION ###

echo Resize all PNGs in this script's folder for all densities?
echo Type current density of those PNGs below. Example: mdpi
set /p inputDensity=

if /i %inputDensity%==ldpi (
	set PERCENT_LDPI=100%%
) else (
	if /i %inputDensity%==mdpi (
		set PERCENT_LDPI=75%%
		set PERCENT_MDPI=100%%
	) else (
		if /i %inputDensity%==hdpi (
			set PERCENT_LDPI=50%%
			set PERCENT_MDPI=66.6666666667%%
			set PERCENT_HDPI=100%%
		) else (
			if /i %inputDensity%==xhdpi (
				set PERCENT_LDPI=37.5%%
				set PERCENT_MDPI=50%%
				set PERCENT_HDPI=75%%
				set PERCENT_XHDPI=100%%
			) else (
				if /i %inputDensity%==xxhdpi (
					set PERCENT_LDPI=25%%
					set PERCENT_MDPI=33.3333333333%%
					set PERCENT_HDPI=50%%
					set PERCENT_XHDPI=66.6666666667%%
					set PERCENT_XXHDPI=100%%
				) else (
					if /i %inputDensity%==xxxhdpi (
						set PERCENT_LDPI=18.75%%
						set PERCENT_MDPI=25%%
						set PERCENT_HDPI=37.5%%
						set PERCENT_XHDPI=50%%
						set PERCENT_XXHDPI=75%%
						set PERCENT_XXXHDPI=100%%
					) else (
						exit
					)
				)
			)
		)
	)
)

echo.

if exist %PATH_LDPI% (
	echo Directory %PATH_LDPI% does already exist.
	echo Shutting down
	pause
	exit
) else (
	mkdir %PATH_LDPI%
)
if exist %PATH_MDPI% (
	echo Directory %PATH_MDPI% does already exist.
	echo Shutting down
	pause
	exit
) else (
	mkdir %PATH_MDPI%
)
if exist %PATH_HDPI% (
	echo Directory %PATH_HDPI% does already exist.
	echo Shutting down
	pause
	exit
) else (
	mkdir %PATH_HDPI%
)
if exist %PATH_XHDPI% (
	echo Directory %PATH_XHDPI% does already exist.
	echo Shutting down
	pause
	exit
) else (
	mkdir %PATH_XHDPI%
)
if exist %PATH_XXHDPI% (
	echo Directory %PATH_XXHDPI% does already exist.
	echo Shutting down
	pause
	exit
) else (
	mkdir %PATH_XXHDPI%
)
if exist %PATH_XXXHDPI% (
	echo Directory %PATH_XXXHDPI% does already exist.
	echo Shutting down
	pause
	exit
) else (
	mkdir %PATH_XXXHDPI%
)

for %%f in (*.png) do (
	echo %%f
	if not [%PERCENT_LDPI%]==[] (
		"%IMAGE_MAGICK_PATH%" "%%f" -resize %PERCENT_LDPI% "%~dp0%PATH_LDPI%\%%f"
	)
	if not [%PERCENT_MDPI%]==[] (
		"%IMAGE_MAGICK_PATH%" "%%f" -resize %PERCENT_MDPI% "%~dp0%PATH_MDPI%\%%f"
	)
	if not [%PERCENT_HDPI%]==[] (
		"%IMAGE_MAGICK_PATH%" "%%f" -resize %PERCENT_HDPI% "%~dp0%PATH_HDPI%\%%f"
	)
	if not [%PERCENT_XHDPI%]==[] (
		"%IMAGE_MAGICK_PATH%" "%%f" -resize %PERCENT_XHDPI% "%~dp0%PATH_XHDPI%\%%f"
	)
	if not [%PERCENT_XXHDPI%]==[] (
		"%IMAGE_MAGICK_PATH%" "%%f" -resize %PERCENT_XXHDPI% "%~dp0%PATH_XXHDPI%\%%f"
	)
	if not [%PERCENT_XXXHDPI%]==[] (
		"%IMAGE_MAGICK_PATH%" "%%f" -resize %PERCENT_XXXHDPI% "%~dp0%PATH_XXXHDPI%\%%f"
	)
)

echo.
echo Finished

pause