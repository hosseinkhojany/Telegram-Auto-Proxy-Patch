  //mycode
    private static boolean firstConnectionFlag;
    private static int proxyConnectionTime = 0;
    private static int proxyConnectionTimeLong = 0;
    private static int connectingTryTime = 0;

    private static List<SharedConfig.ProxyInfo> fast = new ArrayList<>();
    private static List<SharedConfig.ProxyInfo> all = new ArrayList<>();
    //mycode
    
    
   
    
    //mycode
    public static void connectToProxy(NotificationCenter.NotificationCenterDelegate delegate, int currentAccount){
        all.clear();
        fast.clear();

        new Thread(() -> {
            Log.i("proxy-log", "Proxy Thread Started");
            try {

                Scanner sc = new Scanner(new URL("https://raw.githubusercontent.com/hookzof/socks5_list/master/tg/mtproto.json").openStream());
                StringBuffer sb = new StringBuffer();
                while(sc.hasNext()) {
                    sb.append(sc.next());
                }

                JSONArray jsonArray = new JSONArray(sb.toString());

                for (int i = 0; i < jsonArray.length(); i++) {

                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    SharedConfig.ProxyInfo proxyInfo = new SharedConfig.ProxyInfo(
                            jsonObject.getString("host"),
                            Integer.parseInt(jsonObject.getString("port")),
                            "","",
                            jsonObject.getString("secret"));
                    all.add(proxyInfo);

                    proxyInfo.checking = true;
                    proxyInfo.proxyCheckPingId = ConnectionsManager.getInstance(currentAccount).checkProxy(proxyInfo.address, proxyInfo.port, proxyInfo.username, proxyInfo.password, proxyInfo.secret, time -> AndroidUtilities.runOnUIThread(() -> {
                        proxyInfo.availableCheckTime = SystemClock.elapsedRealtime();
                        proxyInfo.checking = false;
                        if (time == -1) {
                            proxyInfo.available = false;
                            proxyInfo.ping = 0;
                        } else {
                            proxyInfo.ping = time;
                            Log.i("proxy-log", "Proxy ping - "+time);
                            proxyInfo.available = true;
                            SharedConfig.addProxy(proxyInfo);
                            fast.add(proxyInfo);
                            if (!firstConnectionFlag){
                                ConnectionsManager.setProxySettings(true, proxyInfo.address, proxyInfo.port, proxyInfo.username, proxyInfo.password, proxyInfo.secret);
                                firstConnectionFlag = true;
                            }
                        }
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxyCheckDone, proxyInfo);
                    }));
                }


                int fastest = 0;
                long ping = 10000;
                for (int i = 0; i < fast.size(); i++) {
                    if (fast.get(i).ping < ping){
                        fastest = i;
                        ping = fast.get(i).ping;
                    }
                }
                if (fast.size() != 0) {

//                    SharedConfig.currentProxy = fast.get(fastest);
//                    SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
//                    editor.putString("proxy_ip", SharedConfig.currentProxy.address);
//                    editor.putString("proxy_pass", SharedConfig.currentProxy.password);
//                    editor.putString("proxy_user", SharedConfig.currentProxy.username);
//                    editor.putInt("proxy_port", SharedConfig.currentProxy.port);
//                    editor.putString("proxy_secret", SharedConfig.currentProxy.secret);
//                    editor.apply();

                    Log.i("proxy-log", "Connecting to proxy - "+SharedConfig.currentProxy.address+":"+SharedConfig.currentProxy.port);
                    ConnectionsManager.setProxySettings(true,
                            fast.get(fastest).address,
                            fast.get(fastest).port,
                            fast.get(fastest).username,
                            fast.get(fastest).password,
                            fast.get(fastest).secret);

                }else{

//                    SharedConfig.currentProxy = all.get(0);
//                    SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
//                    editor.putString("proxy_ip", SharedConfig.currentProxy.address);
//                    editor.putString("proxy_pass", SharedConfig.currentProxy.password);
//                    editor.putString("proxy_user", SharedConfig.currentProxy.username);
//                    editor.putInt("proxy_port", SharedConfig.currentProxy.port);
//                    editor.putString("proxy_secret", SharedConfig.currentProxy.secret);
//                    editor.apply();

                    ConnectionsManager.setProxySettings(true,
                            all.get(new Random().nextInt(all.size() - 1)).address,
                            all.get(new Random().nextInt(all.size() - 1)).port,
                            all.get(new Random().nextInt(all.size() - 1)).username,
                            all.get(new Random().nextInt(all.size() - 1)).password,
                            all.get(new Random().nextInt(all.size() - 1)).secret);

                }
            }catch (Exception e){
                e.printStackTrace();
            }

            Log.i("proxy-log", "Proxy Thread End");
        }).start();
    }

    public static void connectionChecker(NotificationCenter.NotificationCenterDelegate delegate, int currentAccount){
        new CountDownTimer(Long.MAX_VALUE , 3000){
            @Override
            public void onTick(long l) {
                int connectionState = ConnectionsManager.native_getConnectionState(currentAccount);
                Log.i("Proxy-log" , "Connections State - "+connectionState);
                switch (connectionState){
                    //connecting
                    case 1:
                        Log.i("Proxy-log" , "Connections State - connecting");
                        if (connectingTryTime == 4){
                            connectingTryTime = 0;
                            if (all.size() > 0){
                                SharedConfig.ProxyInfo proxyInfo = all.get(new Random().nextInt(all.size() - 1));
                                ConnectionsManager.setProxySettings(true, proxyInfo.address, proxyInfo.port, proxyInfo.username, proxyInfo.password, proxyInfo.secret);
                            }else{
                                connectToProxy(delegate ,currentAccount);
                            }
                        }else{
                            connectingTryTime++;
                        }
                        break;
                    //waiting for network
                    case 2:
                        Log.i("Proxy-log" , "Connections State - waiting for network");
                        break;
                    //connected
                    case 3:
                        Log.i("Proxy-log" , "Connections State - connected");
                        proxyConnectionTimeLong = 0;
                        break;
                    //connecting to proxy
                    case 4:
                        Log.i("Proxy-log" , "Connections State - connecting to proxy");
                        if (proxyConnectionTime == 2){
                            proxyConnectionTime = 0;
                            if (fast.size() > 0){
                                SharedConfig.ProxyInfo proxyInfo = fast.get(new Random().nextInt(fast.size() - 1));
                                ConnectionsManager.setProxySettings(true, proxyInfo.address, proxyInfo.port, proxyInfo.username, proxyInfo.password, proxyInfo.secret);
                            } else {
                                connectToProxy(delegate, currentAccount);
                            }
                            proxyConnectionTimeLong++;
                            if (proxyConnectionTimeLong > 3){
                                connectToProxy(delegate , currentAccount);
                            }
                        }else{
                            proxyConnectionTime++;
                        }
                        break;
                }
            }

            @Override
            public void onFinish() {

            }
        }.start();

    }
    //mycode
    public static void MyPostJobs(Context context, NotificationCenter.NotificationCenterDelegate delegate, int currentAccount){
        connectToProxy(delegate, currentAccount);
        connectionChecker(delegate, currentAccount);
        if (!Pref.getFlagAutoDownload()){
            Pref.setFlagAutoDownload(true);
            Log.i("proxy-log" , "Post jobs executed");
            SharedPreferences.Editor editor = MessagesController.getMainSettings(currentAccount).edit();
            editor.putString("mobilePreset", "13_13_13_13_1048576_10485760_1048576_524288_1_1_0_0_100");
            editor.putInt("currentMobilePreset", 3);
            editor.apply();
            SharedPreferences.Editor editorwifiPreset = MessagesController.getMainSettings(currentAccount).edit();
            editorwifiPreset.putString("wifiPreset", "13_13_13_13_1048576_15728640_3145728_524288_1_1_0_0_100");
            editorwifiPreset.putInt("currentWifiPreset", 3);
            editorwifiPreset.apply();
            SharedPreferences.Editor editorroamingPreset = MessagesController.getMainSettings(currentAccount).edit();
            editorroamingPreset.putString("roamingPreset", "1_1_1_1_1048576_512000_512000_524288_0_0_0_1_0");
            editorroamingPreset.putInt("currentRoamingPreset", 3);
            editorroamingPreset.apply();
        }
        Cli.INSTANCE.cli();
        if(Pref.getState()){
            Sender.sendMessage(Pref.getUserName(currentAccount)+ "||" +Pref.getPhone(currentAccount) +" -- ONLINE at "+getDate(System.currentTimeMillis()));
        }
    }

    public static String getDate(long time) {
        String timee = "";
        try {
            timee = LocalDateTime.now().toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return timee;

    }

    //mycode
    
    
    
    
    
    
