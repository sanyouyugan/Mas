package com.mas.log.mlog

class Aops {

    public static String rootAops = "[" +

            "  {" +
            "    \"dependent\": \"com.squareup.okhttp3:okhttp\"," +
            "    \"clazz\": \"okhttp3.OkHttpClient\$Builder\"," +
            "    \"name\": \"build\"," +
            "    \"params\": [" +
            "      \"okhttp3.OkHttpClient\$Builder.build\"" +
            "    ]" +
            "  }" +


            "]"


    Aops(String clazz, String name, ArrayList<String> args, String dependent) {
        this.clazz = clazz
        this.name = name
        this.params = args
        this.dependent = dependent
    }
    String clazz = ""
    String dependent = ""
    String name = ""
    ArrayList<String> params
}

