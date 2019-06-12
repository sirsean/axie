(ns axie.web3
  (:import (org.web3j.utils Numeric)
           (org.web3j.crypto Keys Sign Sign$SignatureData))
  (:gen-class))

(defn split-signed-msg
  [signed]
  (let [no-0x (subs signed 2)]
    {:r (BigInteger. (subs no-0x 0 64) 16)
     :s (BigInteger. (subs no-0x 64 128) 16)
     :v (BigInteger. (subs no-0x 128) 16)}))

(defn signature-data
  [{:keys [r s v]}]
  (Sign$SignatureData. (byte v)
                       (Numeric/toBytesPadded r 32)
                       (Numeric/toBytesPadded s 32)))

(defn wrap-msg
  [msg]
  (format "\u0019Ethereum Signed Message:\n%d%s" (count msg) msg))

(defn ec-recover
  "Take an unsigned message payload and a signature that is that same message
  signed by someone, and determine the address of whoever signed it."
  [msg signed]
  (str "0x" (Keys/getAddress (Sign/signedMessageToKey
                               (-> msg wrap-msg .getBytes)
                               (-> signed split-signed-msg signature-data)))))
