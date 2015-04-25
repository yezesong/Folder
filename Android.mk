LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
#下面这个选项是生成的apk 是放在system区域还是data区域/tests表示在data区域，optional在system区域
LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := android-common \
launchernetwork

LOCAL_SRC_FILES := $(call all-java-files-under, src)
# LOCAL_AAPT_FLAGS = -c hdpi

LOCAL_PACKAGE_NAME := GameFolder

#platform表示为为系统用户apk编译，shared表示为普通用户编译
LOCAL_CERTIFICATE := platform
#LOCAL_CERTIFICATE := shared

#LOCAL_OVERRIDES_PACKAGES := Home

include $(BUILD_PACKAGE)
  
include $(CLEAR_VARS) 
LOCAL_MODULE_TAGS := optional

#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := launchernetwork:libs/launchernetwork.jar
  
include $(BUILD_MULTI_PREBUILT)

