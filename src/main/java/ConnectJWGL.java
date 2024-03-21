import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ConnectJWGL {

    private final String url = "https://jw.qlu.edu.cn";
    private Map<String,String> cookies = new HashMap<>();
    private String modulus;
    private String exponent;
    private String csrftoken;
    private Connection connection;
    private Connection.Response response;
    private Document document;
    private String stuNum;
    private String password;

    public ConnectJWGL(String stuNum,String password){
        this.stuNum = stuNum;
        this.password = password;
    }

    public void init() throws Exception{
        getCsrftoken();
        getRSApublickey();
    }

    // 获取csrftoken和Cookies
    private void getCsrftoken(){
        try{
            connection = Jsoup.connect(url+ "/jwglxt/xtgl/login_slogin.html");
            connection.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36 Edg/121.0.0.0");
            response = connection.execute();
            cookies = response.cookies();
            //保存csrftoken
            document = Jsoup.parse(response.body());
            csrftoken = document.getElementById("csrftoken").val();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    // 获取公钥并加密密码
    public void getRSApublickey() throws Exception{
        connection = Jsoup.connect(url+ "/jwglxt/xtgl/login_getPublicKey.html?" +
                "time="+ new Date().getTime());
        connection.header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");
        response = connection.cookies(cookies).ignoreContentType(true).execute();
        JSONObject jsonObject = JSON.parseObject(response.body());
        modulus = jsonObject.getString("modulus");
        exponent = jsonObject.getString("exponent");
        password = RSAEncoder.RSAEncrypt(password, B64.b64tohex(modulus), B64.b64tohex(exponent));
        password = B64.hex2b64(password);
    }

    //登录
    public boolean beginLogin() throws Exception{

        connection = Jsoup.connect(url+ "/jwglxt/xtgl/login_slogin.html");
        connection.header("Content-Type","application/x-www-form-urlencoded;charset=utf-8");
        connection.header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");

        connection.data("csrftoken",csrftoken);
        connection.data("yhm",stuNum);
        connection.data("mm",password);
        connection.data("mm",password);
        Connection.Response res = connection.cookies(cookies).ignoreContentType(true)
                .method(Connection.Method.POST).execute();
        cookies = connection.request().cookies();
        cookies.remove("rememberMe");
        response = connection.execute();
        document = Jsoup.parse(response.body());
        if(document.getElementById("tips") == null){
            System.out.println("欢迎登陆");
            return true;
        }else{
            System.out.println(document.getElementById("tips").text());
            return false;
        }
    }

    // 获取课表信息
    public void getStudentTimetable(int year , int term) throws Exception {
        connection = Jsoup.connect(url+ "/jwglxt/kbcx/xskbcx_cxXsKb.html?gnmkdm=N2151");
        connection.header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");
        connection.data("xnm",String.valueOf(year));
        connection.data("xqm",String.valueOf(term * term * 3));
        response = connection.cookies(cookies).method(Connection.Method.POST).ignoreContentType(true).execute();
        JSONObject jsonObject = JSON.parseObject(response.body());
        if(jsonObject.get("kbList") == null){
            System.out.println("暂时没有安排课程");
            return;
        }
        JSONArray timeTable = JSON.parseArray(jsonObject.getString("kbList"));
        System.out.println(String.valueOf(year) + " -- " + String.valueOf(year + 1) + "学年 " + "第" + term + "学期");
        for (Iterator iterator = timeTable.iterator(); iterator.hasNext();) {
            JSONObject lesson = (JSONObject) iterator.next();
            System.out.println(lesson.getString("xqjmc") + " " +
                    lesson.getString("jc") + " " +
                    lesson.getString("kcmc") + " " +
                    lesson.getString("xm") + " " +
                    lesson.getString("xqmc") + " " +
                    lesson.getString("cdmc") + " " +
                    lesson.getString("zcd"));
        }
    }

    // 获取成绩信息
    public void getStudentGrade(int year , int term) throws Exception {
        Map<String,String> datas = new HashMap<>();
        datas.put("xnm",String.valueOf(year));
        datas.put("xqm",String.valueOf(term * term * 3));
        datas.put("_search","false");
        datas.put("nd",String.valueOf(new Date().getTime()));
        datas.put("queryModel.showCount","15");
        datas.put("queryModel.currentPage","1");
        datas.put("queryModel.sortName","");
        datas.put("queryModel.sortOrder","asc");
        datas.put("kcbj","");
        datas.put("time","1");

        connection = Jsoup.connect(url+ "/jwglxt/cjcx/cjcx_cxDgXscj.html?gnmkdm=N305005&layout=default&su=" + stuNum);
        connection.header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");
        response = connection.cookies(cookies)
                .method(Connection.Method.POST)
                .data(datas)
                .ignoreContentType(true)
                .execute();

        connection = Jsoup.connect(url+ "/jwglxt/cjcx/cjcx_cxDgXscj.html?doType=query&gnmkdm=N305005");
        connection.header("Content-Type","application/x-www-form-urlencoded;charset=UTF-8");
        connection.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0");
        response = connection.cookies(cookies).method(Connection.Method.POST)
                .data(datas).ignoreContentType(true).execute();
        JSONObject jsonObject = JSON.parseObject(response.body());
        JSONArray gradeTable = JSON.parseArray(jsonObject.getString("items"));
        for (Iterator iterator = gradeTable.iterator(); iterator.hasNext();) {
            JSONObject lesson = (JSONObject) iterator.next();
            System.out.println(lesson.getString("kcmc") + " " +
                    lesson.getString("jsxm") + " " +
                    lesson.getString("bfzcj") + " " +
                    lesson.getString("jd"));
        }
    }

    public void logout() throws Exception {
        connection = Jsoup.connect(url+ "/jwglxt/logout");
        connection.header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");
        response = connection.cookies(cookies).ignoreContentType(true).execute();
    }

}
