#!/bin/bash
echo "============================================"
echo " UniKart - SHA Certificate Key Generator"
echo "============================================"
echo ""
echo "Getting DEBUG SHA keys (for development):"
echo ""
keytool -list -v \
  -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android 2>/dev/null | grep -E "SHA1|SHA256|MD5"
echo ""
echo "If the above is empty, run:"
echo "keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android"
echo ""
echo "============================================"
echo " Add these SHA keys to Firebase Console:"
echo " Project Settings > Your apps > Add fingerprint"
echo "============================================"
