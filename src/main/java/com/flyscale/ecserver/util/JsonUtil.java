package com.flyscale.ecserver.util;

import android.text.TextUtils;

public class JsonUtil {

    /**
     * 判断是否是JSONObject格式
     * @param json
     * @return
     */
    public static boolean isJsonObj(String json) {
        return isJson(json, 0) && json.startsWith("{");
    }

    /**
     * 判断是否是JSONArray格式
     * @param json
     * @return
     */
    public static boolean isJsonArray(String json){
        return isJson(json, 0) && json.startsWith("[");
    }


    /**
     * 判断Json字符串开始和结束字符
     * @param json
     * @return
     */
    private static boolean isJsonStart(String json) {
        if (!TextUtils.isEmpty(json)) {
            json = json.trim();
            if (json.length() > 1) {
                char s = json.charAt(0);
                char e = json.charAt(json.length() - 1);
                return (s == '{' && e == '}') || (s == '[' && e == ']');
            }
        }
        return false;
    }

    private static class CharState {
        public boolean jsonStart = false;//以 "{"开始了...
        public boolean setDicValue = false;// 可以设置字典值了。
        public boolean escapeChar = false;//以"\"转义符号开始了
        /// <summary>
        /// 数组开始【仅第一开头才算】，值嵌套的以【childrenStart】来标识。
        /// </summary>
        public boolean arrayStart = false;//以"[" 符号开始了
        public boolean childrenStart = false;//子级嵌套开始了。
        /// <summary>
        /// 【0 初始状态，或 遇到“,”逗号】；【1 遇到“：”冒号】
        /// </summary>
        public int state = 0;
        /// <summary>
        /// 【-1 取值结束】【0 未开始】【1 无引号开始】【2 单引号开始】【3 双引号开始】
        /// </summary>
        public int keyStart = 0;
        /// <summary>
        /// 【-1 取值结束】【0 未开始】【1 无引号开始】【2 单引号开始】【3 双引号开始】
        /// </summary>
        public int valueStart = 0;
        public boolean isError = false;//是否语法错误。


        void CheckIsError(char c)//只当成一级处理（因为GetLength会递归到每一个子项处理）
        {
            if (keyStart > 1 || valueStart > 1) {
                return;
            }
            //示例 ["aa",{"bbbb":123,"fff","ddd"}]
            switch (c) {
                case '{'://[{ "[{A}]":[{"[{B}]":3,"m":"C"}]}]
                    isError = jsonStart && state == 0;//重复开始错误 同时不是值处理。
                    break;
                case '}':
                    isError = !jsonStart || (keyStart != 0 && state == 0);//重复结束错误 或者 提前结束{"aa"}。正常的有{}
                    break;
                case '[':
                    isError = arrayStart && state == 0;//重复开始错误
                    break;
                case ']':
                    isError = !arrayStart || jsonStart;//重复开始错误 或者 Json 未结束
                    break;
                case '"':
                case '\'':
                    isError = !(jsonStart || arrayStart); //json 或数组开始。
                    if (!isError) {
                        //重复开始 [""",{"" "}]
                        isError = (state == 0 && keyStart == -1) || (state == 1 && valueStart == -1);
                    }
                    if (!isError && arrayStart && !jsonStart && c == '\'')//['aa',{}]
                    {
                        isError = true;
                    }
                    break;
                case ':':
                    isError = !jsonStart || state == 1;//重复出现。
                    break;
                case ',':
                    isError = !(jsonStart || arrayStart); //json 或数组开始。
                    if (!isError) {
                        if (jsonStart) {
                            isError = state == 0 || (state == 1 && valueStart > 1);//重复出现。
                        } else if (arrayStart)//["aa,] [,]  [{},{}]
                        {
                            isError = keyStart == 0 && !setDicValue;
                        }
                    }
                    break;
                case ' ':
                case '\r':
                case '\n'://[ "a",\r\n{} ]
                case '\0':
                case '\t':
                    break;
                default: //值开头。。
                    isError = (!jsonStart && !arrayStart) || (state == 0 && keyStart == -1) || (valueStart == -1 && state == 1);//
                    break;
            }
        }
    }

    /**
     * 对提供调用的接口
     * @param json
     * @param errIndex
     * @return
     */
    public static boolean isJson(String json, int errIndex) {
        errIndex = 0;
        if (isJsonStart(json)) {
            CharState cs = new CharState();
            char c;
            for (int i = 0; i < json.length(); i++) {
                c = json.charAt(i);
                if (setCharState(c, cs) && cs.childrenStart)//设置关键符号状态。
                {
                    String item = json.substring(i);
                    int err = 0;
                    int length = getValueLength(item, true, err);
                    cs.childrenStart = false;
                    if (err > 0) {
                        errIndex = i + err;
                        return false;
                    }
                    i = i + length - 1;
                }
                if (cs.isError) {
                    errIndex = i;
                    return false;
                }
            }

            return !cs.arrayStart && !cs.jsonStart;
        }
        return false;
    }

    private static boolean setCharState(char c, CharState cs) {
        cs.CheckIsError(c);
        switch (c) {
            case '{'://[{ "[{A}]":[{"[{B}]":3,"m":"C"}]}]
                //region 大括号
                if (cs.keyStart <= 0 && cs.valueStart <= 0) {
                    cs.keyStart = 0;
                    cs.valueStart = 0;
                    if (cs.jsonStart && cs.state == 1) {
                        cs.childrenStart = true;
                    } else {
                        cs.state = 0;
                    }
                    cs.jsonStart = true;//开始。
                    return true;
                }
                //endregion
                break;
            case '}':
                //region 大括号结束
                if (cs.keyStart <= 0 && cs.valueStart < 2 && cs.jsonStart) {
                    cs.jsonStart = false;//正常结束。
                    cs.state = 0;
                    cs.keyStart = 0;
                    cs.valueStart = 0;
                    cs.setDicValue = true;
                    return true;
                }
                // cs.isError = !cs.jsonStart && cs.state == 0;
                //endregion
                break;
            case '[':
                //region 中括号开始
                if (!cs.jsonStart) {
                    cs.arrayStart = true;
                    return true;
                } else if (cs.jsonStart && cs.state == 1) {
                    cs.childrenStart = true;
                    return true;
                }
                //#endregion
                break;
            case ']':
                // #region 中括号结束
                if (cs.arrayStart && !cs.jsonStart && cs.keyStart <= 2 && cs.valueStart <= 0)//[{},333]//这样结束。
                {
                    cs.keyStart = 0;
                    cs.valueStart = 0;
                    cs.arrayStart = false;
                    return true;
                }
                //  #endregion
                break;
            case '"':
            case '\'':
                // #region 引号
                if (cs.jsonStart || cs.arrayStart) {
                    if (cs.state == 0)//key阶段,有可能是数组["aa",{}]
                    {
                        if (cs.keyStart <= 0) {
                            cs.keyStart = (c == '"' ? 3 : 2);
                            return true;
                        } else if ((cs.keyStart == 2 && c == '\'') || (cs.keyStart == 3 && c == '"')) {
                            if (!cs.escapeChar) {
                                cs.keyStart = -1;
                                return true;
                            } else {
                                cs.escapeChar = false;
                            }
                        }
                    } else if (cs.state == 1 && cs.jsonStart)//值阶段必须是Json开始了。
                    {
                        if (cs.valueStart <= 0) {
                            cs.valueStart = (c == '"' ? 3 : 2);
                            return true;
                        } else if ((cs.valueStart == 2 && c == '\'') || (cs.valueStart == 3 && c == '"')) {
                            if (!cs.escapeChar) {
                                cs.valueStart = -1;
                                return true;
                            } else {
                                cs.escapeChar = false;
                            }
                        }

                    }
                }
                //  #endregion
                break;
            case ':':
                //  #region 冒号
                if (cs.jsonStart && cs.keyStart < 2 && cs.valueStart < 2 && cs.state == 0) {
                    if (cs.keyStart == 1) {
                        cs.keyStart = -1;
                    }
                    cs.state = 1;
                    return true;
                }
                // cs.isError = !cs.jsonStart || (cs.keyStart < 2 && cs.valueStart < 2 && cs.state == 1);
                //  #endregion
                break;
            case ',':
                //   #region 逗号 //["aa",{aa:12,}]

                if (cs.jsonStart) {
                    if (cs.keyStart < 2 && cs.valueStart < 2 && cs.state == 1) {
                        cs.state = 0;
                        cs.keyStart = 0;
                        cs.valueStart = 0;
                        //if (cs.valueStart == 1)
                        //{
                        //    cs.valueStart = 0;
                        //}
                        cs.setDicValue = true;
                        return true;
                    }
                } else if (cs.arrayStart && cs.keyStart <= 2) {
                    cs.keyStart = 0;
                    //if (cs.keyStart == 1)
                    //{
                    //    cs.keyStart = -1;
                    //}
                    return true;
                }
                // #endregion
                break;
            case ' ':
            case '\r':
            case '\n'://[ "a",\r\n{} ]
            case '\0':
            case '\t':
                if (cs.keyStart <= 0 && cs.valueStart <= 0) //cs.jsonStart &&
                {
                    return true;//跳过空格。
                }
                break;
            default: //值开头。。
                if (c == '\\') //转义符号
                {
                    if (cs.escapeChar) {
                        cs.escapeChar = false;
                    } else {
                        cs.escapeChar = true;
                        return true;
                    }
                } else {
                    cs.escapeChar = false;
                }
                if (cs.jsonStart || cs.arrayStart) // Json 或数组开始了。
                {
                    if (cs.keyStart <= 0 && cs.state == 0) {
                        cs.keyStart = 1;//无引号的
                    } else if (cs.valueStart <= 0 && cs.state == 1 && cs.jsonStart)//只有Json开始才有值。
                    {
                        cs.valueStart = 1;//无引号的
                    }
                }
                break;
        }
        return false;
    }

    private static int getValueLength(String json, boolean breakOnErr, int errIndex) {
        errIndex = 0;
        int len = 0;
        if (!TextUtils.isEmpty(json)) {
            CharState cs = new CharState();
            char c;
            for (int i = 0; i < json.length(); i++) {
                c = json.charAt(i);
                if (!setCharState(c, cs)) {//设置关键符号状态。
                    if (!cs.jsonStart && !cs.arrayStart) {//json结束，又不是数组，则退出。
                        break;
                    }
                } else if (cs.childrenStart) {//正常字符，值状态下。
                    int length = getValueLength(json.substring(i), breakOnErr, errIndex);//递归子值，返回一个长度。。。
                    cs.childrenStart = false;
                    cs.valueStart = 0;
                    //cs.state = 0;
                    i = i + length - 1;
                }
                if (breakOnErr && cs.isError) {
                    errIndex = i;
                    return i;
                }
                if (!cs.jsonStart && !cs.arrayStart)//记录当前结束位置。
                {
                    len = i + 1;//长度比索引+1
                    break;
                }
            }
        }
        return len;
    }
}
