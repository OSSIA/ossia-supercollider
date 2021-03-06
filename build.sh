  #!/bin/bash -eux

  export OFFLINE=0
  export CUSTOM_BOOST=0
  export OS_IS_LINUX=0
  export OS_IS_OSX=0
  export BOOST_ROOT=""
  export BOOST_INCLUDE=""
  export BOOST_LIBS=""
  export QT_PATH=""
  export DISTRO=""

  if [ "$1" = "offline" ]; then
      OFFLINE=1
  fi

  if ! [ -z "$2" ] && ! [  -z "$3" ]; then
      echo "BOOST CUSTOM"
      CUSTOM_BOOST=1
      BOOST_INCLUDE=$2
      BOOST_LIBS=$3
  fi

  # PLATFORM ----------------------------------------------------------------------------

  UNAME=$(uname | tr "[:upper:]" "[:lower:]")
  # If Linux, try to determine specific distribution
  if [ "$UNAME" == "linux" ]; then
      # If available, use LSB to identify distribution
      if [ -f /etc/lsb-release -o -d /etc/lsb-release.d ]; then
          DISTRO=$(lsb_release -i | cut -d: -f2 | sed s/'^\t'//)
      # Otherwise, use release info file
      else
          DISTRO=$(ls -d /etc/[A-Za-z]*[_-][rv]e[lr]* | grep -v "lsb" | cut -d'/' -f3 | cut -d'-' -f1 | cut -d'_' -f1)
      fi
  fi
  # For everything else (or if above failed), just use generic identifier
  [ "$DISTRO" == "" ] && DISTRO=$UNAME
  unset UNAME

  # DEPENDENCIES ------------------------------------------------------------------------

  nonPackBoost() {

	# installing non-packaged dependencies
    if [ ! -d "dependencies" ]; then
        mkdir dependencies
    fi
        
     (

       cd dependencies

        # download and install boost
        if [ $CUSTOM_BOOST = 0 ]; then

            if [ ! -d "boost_1_65_1" ]; then	
  	            wget http://downloads.sourceforge.net/project/boost/boost/1.65.1/boost_1_65_1.tar.bz2
    	        tar xf boost_1_65_1.tar.bz2
            fi

            if [[ ! -d "boost" ]]; then
  	            ( 
  	            mkdir boost
  	            cd boost_1_65_1
  	            ./bootstrap.sh --with-libraries=atomic,date_time,chrono,exception,timer,thread,system,filesystem,program_options,regex,test \
                    --prefix=../boost
  	            ./b2 install
  	            rm -rf ../boost_1_65_1.tar.bz2
  	            )
            fi
        
        fi
        
       ) 
       
     BOOST_ROOT="$(pwd)/dependencies/boost_1_65_1"
     echo "boost found"
     BOOST_LIBS="$(pwd)/dependencies/boost/lib"	
     BOOST_INCLUDE="$(pwd)/dependencies/boost/include"

}

  if [ "$DISTRO" = "darwin" ]; then

    # check homebrew installation
    export HOMEBREW_BIN=$(command -v brew)
    if [[ "$HOMEBREW_BIN" == "" ]]; then
      echo "Homebrew is not installed."
      echo "Please install it by running the following command:"
      echo '    /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"'
      exit 1
    fi

    # CHECK HOMEBREW DEPENDENCIE
    
    if [ $OFFLINE = 0 ]; then

      # QT 5.5 (required for supercollider's webkit
      if brew ls --versions qt@5.5 > /dev/null; then
        echo "qt@5.5 with brew correctly installed"
      else
        echo "installing qt@5.5 with homebrew"
        brew install qt@5.5
      fi

    # use boost dowloaded source instead of brew package 

      # BOOST >= 1.65
      #if brew ls --versions boost > /dev/null; then
      #  echo "boost already installed"
      #else
      #  echo "installing boost with homebrew"
      #  brew install boost
      #fi
    
      # wget >= 1.19.5
      if brew ls --versions wget > /dev/null; then
         echo "wget already installed"
      else
         echo "installing wget with homebrew"
         brew install wget
      fi  
 
      # CMAKE
      if brew ls --versions cmake > /dev/null; then
        echo "cmake already installed"
      else
        echo "installing cmake with homebrew"
        brew install cmake
      fi

      # ADDING DEFAULT PATHS
      #BOOST_ROOT="/usr/local/opt/boost"
      #BOOST_INCLUDE="/usr/local/opt/boost/include"
      QT_PATH="/usr/local/opt/qt@5.5"

    fi

    # use boost dowloaded source instead of brew package 
 
    nonPackBoost

  elif [ "$DISTRO" = "Ubuntu" ] || [ "$DISTRO" = "elementary" ] ; then

    # checking/installing ossia & supercollider dependencies
    
    if [ $OFFLINE = 0 ]; then
        sudo apt -y install cmake libjack-jackd2-dev libsndfile1-dev libxt-dev libfftw3-dev libudev-dev \
        qt5-default qt5-qmake qttools5-dev qttools5-dev-tools qtdeclarative5-dev libqt5webkit5-dev \
        qtpositioning5-dev libqt5sensors5-dev libqt5opengl5-dev \
        libavahi-compat-libdnssd-dev git wget gcc
     
        QT_PATH="/usr/lib/x86_64-linux-gnu"	

     fi
     
     nonPackBoost
    
  elif [ "$DISTRO" = "archlinux" ]; then
    sudo pacman -S git cmake 

    BOOST_LIBS="/usr/lib"
    BOOST_INCLUDE="/usr/include"
    
  fi
  
  # LIBOSSIA OVERWRITE ------------------------------------------------------------------------

  (

  cd submodules/libossia

  # revert to boost 1.65.1
  yes | cp -rf ../../Ossia/Overwrites/libossia/OssiaDeps.cmake CMake/

  # boost install script not needed, especialy as this downloads a different boost version than 1.65.1 used here
  rm -rf OSSIA/InstallBoost.cmake 

  )

  # LIBOSSIA BUILD ------------------------------------------------------------------------------

  # temporary fix for ossia cxx_standard

  mkdir -p build
  mkdir -p build/libossia
  mkdir -p install
  
  (
    cd build/libossia

    # CMake and build libossia
    echo "now building libossia..."
    
    if [ "$DISTRO" = "darwin" ]; then

        cmake ../../submodules/libossia -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=../../install/libossia -DOSSIA_PYTHON=0 -DOSSIA_NO_QT=1 -DOSSIA_TESTING=0 -DOSSIA_STATIC=1 -DOSSIA_NO_SONAME=1 -DOSSIA_PD=0 -DBOOST_ROOT=$BOOST_ROOT

    elif [ "$DISTRO" = "Ubuntu" ] || [ "$DISTRO" = "elementary" ]; then
     	
	    cmake ../../submodules/libossia -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=../../install/libossia -DOSSIA_PYTHON=0 -DOSSIA_NO_QT=1 -DOSSIA_TESTING=0 -DOSSIA_STATIC=1 -DOSSIA_NO_SONAME=1 -DOSSIA_PD=0 -DBOOST_ROOT=$BOOST_ROOT -DCMAKE_C_COMPILER=/usr/bin/gcc -DCMAKE_CXX_COMPILER=/usr/bin/g++ 
  
    fi

   make -j8
   echo "libossia built succesfully... installing"
   make install

    # Cleaning up
    rm -rf libossia build

  )

  # COPY & OVERWRITES ------------------------------------------------------------------------
  (

  cd submodules/supercollider

  if [ "$1" != "offline" ]; then
      git checkout 3.9
  fi

  cd SCClassLibrary
  if [[ ! -d "Ossia" ]]; then
      mkdir Ossia
  fi
  
  )

  (
  
  yes | cp -rf Ossia/Overwrites/doc/lex.scdoc.cpp submodules/supercollider/SCDoc  
  yes | cp -rf Ossia/Overwrites/root/CMakeLists.txt submodules/supercollider
  yes | cp -rf Ossia/Overwrites/lang/CMakeLists.txt submodules/supercollider/lang
  yes | cp -rf Ossia/Overwrites/lang/PyrPrimitive.cpp submodules/supercollider/lang/LangPrimSource
  yes | cp -rf Ossia/Overwrites/testsuite/CMakeLists.txt submodules/supercollider/testsuite/supernova
  yes | cp -rf Ossia/Overwrites/server/scsynth/CMakeLists.txt submodules/supercollider/server/scsynth
  yes | cp -rf Ossia/Overwrites/server/supernova/CMakeLists.txt submodules/supercollider/server/supernova
  yes | cp -rf Ossia/Overwrites/external_libraries/CMakeLists.txt submodules/supercollider/external_libraries
  yes | cp -rf Ossia/Overwrites/editors/CMakeLists.txt submodules/supercollider/editors/sc-ide

  yes | cp -rf Ossia/Classes/ossia.sc submodules/supercollider/SCClassLibrary/Ossia
  yes | cp -rf Ossia/HelpSource/Guides/OssiaReference.schelp submodules/supercollider/HelpSource/Guides
  yes | cp -rf Ossia/HelpSource/Classes submodules/supercollider/HelpSource/Classes
  yes | cp -rf Ossia/HelpSource/Help.schelp submodules/supercollider/HelpSource

  shopt -s dotglob nullglob
  mv submodules/supercollider/HelpSource/Classes/Classes submodules/supercollider/HelpSource/Classes/Ossia
  mv submodules/supercollider/HelpSource/Classes/Ossia/* submodules/supercollider/HelpSource/Classes/
  rm -rf submodules/supercollider/HelpSource/Classes/Ossia

  cd install/libossia/include
  if [[ ! -d "ossia-sc" ]]; then
      mkdir ossia-sc
  fi
  
  )

  # move the ossia prim header into ossia include directory... 
  # the header should maybe be present in libossia repository...
  yes | cp -rf Ossia/Primitives/pyrossiaprim.h install/libossia/include/ossia-sc

  # SUPERCOLLIDER BUILD ----------------------------------------------------------------------
  (
  
  cd submodules/supercollider

  # remove packaged boost, which gets somehow included even with SYSTEM_BOOST=ON
  rm -rf external_libraries/boost
  
  )

  mkdir -p build/supercollider
  cd build/supercollider

  if [ "$DISTRO" = "darwin" ]; then 
      
	cmake ../../submodules/supercollider -DCMAKE_PREFIX_PATH=$QT_PATH -DCMAKE_INSTALL_PREFIX=../../install/supercollider -DSYSTEM_BOOST=ON -DBOOST_ROOT=$BOOST_ROOT -DBOOST_INCLUDEDIR=$BOOST_INCLUDE -DBOOST_LIBRARYDIR=$BOOST_LIBS -DBoost_DEBUG=OFF 
	

  elif [ "$DISTRO" = "Ubuntu" ] || [ "$DISTRO" = "elementary" ]; then

    
	cmake ../../submodules/supercollider -DCMAKE_C_COMPILER=/usr/bin/gcc -DCMAKE_CXX_COMPILER=/usr/bin/g++ -DCMAKE_BUILD_TYPE=Release -DSYSTEM_BOOST=ON -DBOOST_INCLUDEDIR=$BOOST_INCLUDE -DBOOST_LIBRARYDIR=$BOOST_LIBS -DBOOST_ROOT=$BOOST_ROOT -DBoost_DEBUG=OFF
  
  fi

  make -j8
  sudo make install


