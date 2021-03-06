/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 2.0.10
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.badlogic.gdx.physics.bullet;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;

public class btElement {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected btElement(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  public static long getCPtr(btElement obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        gdxBulletJNI.delete_btElement(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setM_id(int value) {
    gdxBulletJNI.btElement_m_id_set(swigCPtr, this, value);
  }

  public int getM_id() {
    return gdxBulletJNI.btElement_m_id_get(swigCPtr, this);
  }

  public void setM_sz(int value) {
    gdxBulletJNI.btElement_m_sz_set(swigCPtr, this, value);
  }

  public int getM_sz() {
    return gdxBulletJNI.btElement_m_sz_get(swigCPtr, this);
  }

  public btElement() {
    this(gdxBulletJNI.new_btElement(), true);
  }

}
