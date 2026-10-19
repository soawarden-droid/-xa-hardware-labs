package com.xa.phantom;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.Intent;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;

public class MainActivity extends Activity {
    private final int BG=Color.rgb(8,12,22), PANEL=Color.rgb(17,25,39), CYAN=Color.rgb(76,225,233), WHITE=Color.rgb(228,239,248), MUTED=Color.rgb(154,171,188);
    private LinearLayout root;
    private TextView output;
    private EditText hostField, portField, consoleField;
    private StringBuilder report=new StringBuilder();
    private volatile boolean busy=false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
            try {
                StringWriter sw=new StringWriter();
                error.printStackTrace(new PrintWriter(sw));
                getPreferences(0).edit().putString("crash",sw.toString()).commit();
            } catch(Exception ignored) {}
            android.os.Process.killProcess(android.os.Process.myPid());
        });
        try { buildUi(); }
        catch(Throwable t) { emergency(t); }
    }

    private GradientDrawable shape(int color) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(color); d.setCornerRadius(18);
        d.setStroke(1,Color.rgb(34,57,72)); return d;
    }
    private TextView text(String s,int size,int color) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setPadding(4,9,4,9); return t;
    }
    private void addButton(String title,Runnable action) {
        Button b=new Button(this);
        b.setText(title); b.setAllCaps(false); b.setTextColor(WHITE); b.setTextSize(14);
        b.setBackground(shape(PANEL));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(49));
        lp.setMargins(0,0,0,dp(9)); root.addView(b,lp);
        b.setOnClickListener(v->action.run());
    }
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
    private void buildUi(){
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(26),dp(18),dp(28));root.setBackgroundColor(BG);
        scroll.addView(root);setContentView(scroll);
        TextView title=text("XA / PHANTOM",29,CYAN);title.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);root.addView(title);
        root.addView(text("MOBILE CYBERSECURITY LAB  •  v1.0",12,MUTED));
        root.addView(text("Real diagnostics. No root. No cloud account.",13,WHITE));
        root.addView(text("01  DEVICE INTELLIGENCE",14,CYAN));
        addButton("Device security audit",this::deviceAudit);
        addButton("Network snapshot",this::networkSnapshot);
        root.addView(text("02  AUTHORIZED HOST TESTS",14,CYAN));
        root.addView(text("Only test systems you own or have permission to inspect.",12,MUTED));
        hostField=new EditText(this);hostField.setSingleLine(true);
        hostField.setText("example.com");hostField.setTextColor(WHITE);
        hostField.setHint("Host or IP address");hostField.setHintTextColor(MUTED);
        root.addView(hostField);
        portField=new EditText(this);portField.setSingleLine(true);
        portField.setInputType(2);portField.setText("443");portField.setTextColor(WHITE);
        portField.setHint("TCP port 1–65535");portField.setHintTextColor(MUTED);
        root.addView(portField);
        addButton("Resolve DNS",()->start("DNS LOOKUP",()->dns(readHost())));
        addButton("Test single TCP port",()->start("TCP CONNECT",()->tcp(readHost(),readPort())));
        addButton("Inspect TLS certificate",()->start("TLS CERTIFICATE",()->tls(readHost())));
        root.addView(text("03  PHANTOM COMMAND CONSOLE",14,CYAN));
        root.addView(text("Commands: help, device, net, dns HOST, tcp HOST PORT, tls HOST, clear, export",12,MUTED));
        consoleField=new EditText(this);consoleField.setSingleLine(true);consoleField.setTextColor(WHITE);consoleField.setHintTextColor(MUTED);consoleField.setHint("Type a command...");root.addView(consoleField);
        addButton("Execute command",this::executeCommand);
        root.addView(text("04  REPORT CONSOLE",14,CYAN));
        output=text("READY • Choose a diagnostic above.",13,WHITE);
        output.setTypeface(Typeface.MONOSPACE);output.setTextIsSelectable(true);
        output.setPadding(dp(14),dp(14),dp(14),dp(14));output.setBackground(shape(PANEL));
        root.addView(output,new LinearLayout.LayoutParams(-1,-2));
        addButton("Share diagnostic report",this::shareReport);
        addButton("Clear report",()->{report.setLength(0);output.setText("READY • Report cleared.");});
        String previous=getPreferences(0).getString("crash","");
        if(!previous.isEmpty()) {
            report.append("PREVIOUS CRASH\n").append(previous);
            output.setText(report.toString());
            getPreferences(0).edit().remove("crash").apply();
        }
    }
    private void emergency(Throwable t){
        TextView v=text("XA PHANTOM STARTUP ERROR\n\n"+android.util.Log.getStackTraceString(t),14,Color.RED);
        v.setTextIsSelectable(true);setContentView(v);
    }
    private String readHost(){
        String s=hostField.getText().toString().trim();
        if(s.length()==0 || s.length()>253 || !s.matches("[a-zA-Z0-9.:_-]+"))throw new IllegalArgumentException("Enter a valid hostname or IP (no URL/path).");
        return s;
    }
    private int readPort(){
        int p=Integer.parseInt(portField.getText().toString().trim());
        if(p<1||p>65535)throw new IllegalArgumentException("Port must be 1–65535");
        return p;
    }
    private interface Job{String run() throws Exception;}
    private void start(String label,Job job){
        if(busy){Toast.makeText(this,"A diagnostic is already running",Toast.LENGTH_SHORT).show();return;}
        busy=true;output.setText(label+" • Working...");
        new Thread(()->{
            String result;
            try {result=job.run();}
            catch(Exception e){result="ERROR: "+e.getClass().getSimpleName()+": "+e.getMessage();}
            String message="["+new SimpleDateFormat("HH:mm:ss",Locale.US).format(new Date())+"] "+label+"\n"+result+"\n\n";
            runOnUiThread(()->{report.append(message);output.setText(report.toString());busy=false;});
        },"xa-diagnostic").start();
    }
    private void deviceAudit(){
        StringBuilder s=new StringBuilder();
        s.append("Manufacturer: ").append(Build.MANUFACTURER).append("\nModel: ").append(Build.MODEL)
         .append("\nAndroid: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")")
         .append("\nSecurity patch: ").append(Build.VERSION.SECURITY_PATCH)
         .append("\nBuild: ").append(Build.DISPLAY)
         .append("\nABIs: ").append(Arrays.toString(Build.SUPPORTED_ABIS))
         .append("\nRoot status: not assessed (requires additional checks)")
         .append("\nNote: patch date does not prove the device is secure.");
        appendLocal("DEVICE AUDIT",s.toString());
    }
    private void networkSnapshot(){
        StringBuilder s=new StringBuilder();
        try {
            ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni=cm==null?null:cm.getActiveNetworkInfo();
            s.append("Active network: ").append(ni==null?"Unavailable":ni.getTypeName()+" / "+ni.getState()).append("\n");
            Enumeration<NetworkInterface> all=NetworkInterface.getNetworkInterfaces();
            if(all!=null)while(all.hasMoreElements()){
                NetworkInterface n=all.nextElement();
                s.append("\n").append(n.getName()).append(" [").append(n.isUp()?"UP":"DOWN").append("]");
                Enumeration<InetAddress> addresses=n.getInetAddresses();
                while(addresses.hasMoreElements())s.append("\n  ").append(addresses.nextElement().getHostAddress());
            }
        }catch(Exception e){s.append("\nRestricted: ").append(e.getMessage());}
        appendLocal("NETWORK SNAPSHOT",s.toString());
    }
    private void appendLocal(String title,String data){
        String msg="["+new SimpleDateFormat("HH:mm:ss",Locale.US).format(new Date())+"] "+title+"\n"+data+"\n\n";
        report.append(msg);output.setText(report.toString());
    }
    private String dns(String host)throws Exception{
        StringBuilder s=new StringBuilder("Host: "+host);
        InetAddress[] addresses=InetAddress.getAllByName(host);
        for(InetAddress a:addresses)s.append("\n").append(a.getHostAddress());
        return s.toString();
    }
    private String tcp(String host,int port)throws Exception{
        long t=System.nanoTime();
        try(Socket socket=new Socket()){
            socket.connect(new InetSocketAddress(host,port),3000);
            long ms=(System.nanoTime()-t)/1000000;
            return "Host: "+host+"\nPort: "+port+"\nTCP connection: SUCCESS\nElapsed: "+ms+" ms\nA reachable port is NOT proof of a vulnerability.";
        }catch(IOException e){return "Host: "+host+"\nPort: "+port+"\nTCP connection: FAILED\nReason: "+e.getMessage();}
    }
    private String tls(String host)throws Exception{
        if(host.contains(":"))throw new IllegalArgumentException("TLS inspector expects a DNS hostname (not IPv6 literal)");
        SSLSocketFactory f=(SSLSocketFactory)SSLSocketFactory.getDefault();
        try(SSLSocket s=(SSLSocket)f.createSocket()){
            s.connect(new InetSocketAddress(host,443),4000);
            s.setSoTimeout(4000);
            SSLParameters params=s.getSSLParameters();
            params.setEndpointIdentificationAlgorithm("HTTPS");
            s.setSSLParameters(params);
            s.startHandshake();
            SSLSession session=s.getSession();
            StringBuilder out=new StringBuilder("Host: "+host+"\nProtocol: "+session.getProtocol()+"\nCipher: "+session.getCipherSuite());
            for(Certificate c:session.getPeerCertificates()){
                if(c instanceof X509Certificate){
                    X509Certificate cert=(X509Certificate)c;
                    out.append("\nSubject: ").append(cert.getSubjectX500Principal())
                       .append("\nIssuer: ").append(cert.getIssuerX500Principal())
                       .append("\nValid from: ").append(cert.getNotBefore())
                       .append("\nValid until: ").append(cert.getNotAfter());
                    break;
                }
            }
            return out.toString();
        }
    }
    private void executeCommand(){
        String line=consoleField.getText().toString().trim();
        if(line.isEmpty())return;
        consoleField.setText("");
        String[] a=line.split("\\s+");
        String cmd=a[0].toLowerCase(Locale.US);
        try {
            switch(cmd){
                case "help": appendLocal("HELP","device | net | dns HOST | tcp HOST PORT | tls HOST | clear | export");break;
                case "device":deviceAudit();break;
                case "net":networkSnapshot();break;
                case "dns":if(a.length!=2)throw new IllegalArgumentException("Usage: dns HOST");start("CMD "+line,()->dns(validateHost(a[1])));break;
                case "tcp":if(a.length!=3)throw new IllegalArgumentException("Usage: tcp HOST PORT");start("CMD "+line,()->tcp(validateHost(a[1]),validatePort(a[2])));break;
                case "tls":if(a.length!=2)throw new IllegalArgumentException("Usage: tls HOST");start("CMD "+line,()->tls(validateHost(a[1])));break;
                case "clear":report.setLength(0);output.setText("READY • Report cleared.");break;
                case "export":shareReport();break;
                default:appendLocal("UNKNOWN COMMAND",cmd+" — type help");
            }
        }catch(Exception e){appendLocal("COMMAND ERROR",e.getMessage());}
    }
    private String validateHost(String h){if(h.length()==0||h.length()>253||!h.matches("[a-zA-Z0-9.:_-]+"))throw new IllegalArgumentException("Invalid host");return h;}
    private int validatePort(String s){int p=Integer.parseInt(s);if(p<1||p>65535)throw new IllegalArgumentException("Port must be 1–65535");return p;}
    private void shareReport(){
        if(report.length()==0){Toast.makeText(this,"Run a diagnostic first",Toast.LENGTH_SHORT).show();return;}
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT,"XA PHANTOM diagnostics");
        i.putExtra(Intent.EXTRA_TEXT,report.toString());
        startActivity(Intent.createChooser(i,"Share report"));
    }
}
