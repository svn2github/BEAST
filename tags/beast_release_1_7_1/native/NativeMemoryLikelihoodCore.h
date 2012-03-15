/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore */

#ifndef _Included_dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore
#define _Included_dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore
 * Method:    allocateNativeMemoryArray
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore_allocateNativeMemoryArray
  (JNIEnv *, jobject, jint);
  
JNIEXPORT jlong JNICALL Java_dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore_allocateNativeIntMemoryArray
  (JNIEnv *, jobject, jint);


/*
 * Class:     dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore
 * Method:    freeNativeMemoryArray
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore_freeNativeMemoryArray
  (JNIEnv *, jobject, jlong);

/*
 * Class:     dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore
 * Method:    setNativeMemoryArray
 * Signature: ([DIJII)V
 */
JNIEXPORT void JNICALL Java_dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore_setNativeMemoryArray___3DIJII
  (JNIEnv *, jobject, jdoubleArray, jint, jlong, jint, jint);

/*
 * Class:     dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore
 * Method:    setNativeMemoryArray
 * Signature: ([IIJII)V
 */
JNIEXPORT void JNICALL Java_dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore_setNativeMemoryArray___3IIJII
  (JNIEnv *, jobject, jintArray, jint, jlong, jint, jint);

/*
 * Class:     dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore
 * Method:    getNativeMemoryArray
 * Signature: (JI[DII)V
 */
JNIEXPORT void JNICALL Java_dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore_getNativeMemoryArray__JI_3DII
  (JNIEnv *, jobject, jlong, jint, jdoubleArray, jint, jint);

/*
 * Class:     dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore
 * Method:    getNativeMemoryArray
 * Signature: (JI[III)V
 */
JNIEXPORT void JNICALL Java_dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore_getNativeMemoryArray__JI_3III
  (JNIEnv *, jobject, jlong, jint, jintArray, jint, jint);

/*
 * Class:     dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore
 * Method:    nativeIntegratePartials
 * Signature: (J[DII[DI)V
 */
JNIEXPORT void JNICALL Java_dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore_nativeIntegratePartials
  (JNIEnv *, jobject, jlong, jdoubleArray, jint, jint, jdoubleArray, jint);

/*
 * Class:     dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore
 * Method:    nativePartialsPartialsPruning
 * Signature: (JJJJIIJI)V
 */
JNIEXPORT void JNICALL Java_dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore_nativePartialsPartialsPruning
  (JNIEnv *, jobject, jlong, jlong, jlong, jlong, jint, jint, jlong, jint);

/*
 * Class:     dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore
 * Method:    nativeStatesPartialsPruning
 * Signature: (JJJJIIJI)V
 */
JNIEXPORT void JNICALL Java_dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore_nativeStatesPartialsPruning
  (JNIEnv *, jobject, jlong, jlong, jlong, jlong, jint, jint, jlong, jint);

/*
 * Class:     dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore
 * Method:    nativeStatesStatesPruning
 * Signature: (JJJJIIJI)V
 */
JNIEXPORT void JNICALL Java_dr_evomodel_treelikelihood_NativeMemoryLikelihoodCore_nativeStatesStatesPruning
  (JNIEnv *, jobject, jlong, jlong, jlong, jlong, jint, jint, jlong, jint);

#ifdef __cplusplus
}
#endif
#endif
