# Status report persistence fix

StatusBridge.call is synchronized to prevent lost increments from concurrent reports. The provider now returns accepted=true only after SharedPreferences.commit() confirms persistence. SystemUiEntry retains its retry behavior on a false or missing acknowledgement. Counts are accepted reports, not LSPosed scope or injection counts. Android compilation and device tests remain outstanding.
