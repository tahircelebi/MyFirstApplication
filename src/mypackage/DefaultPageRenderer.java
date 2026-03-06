package mypackage;

/**
 * Renders login and welcome pages as server-side HTML strings.
 */
public class DefaultPageRenderer implements PageRenderer {
    /**
     * Builds the login page.
     *
     * @param warning optional warning message shown above the form
     * @return html for login page
     */
    @Override
    public String renderLoginPage(String warning) {
        String warningHtml = "";
        if (warning != null && !warning.isEmpty()) {
            warningHtml = "<div class=\"warning\">" + WebDataUtils.escapeHtml(warning) + "</div>";
        }

        return "<!doctype html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n"
                + "  <title>Authorization</title>\n"
                + "  <style>\n"
                + "    :root { --li-blue:#0a66c2; --li-bg:#f3f2ef; --li-surface:#ffffff; --li-text:#1f1f1f; --li-sub:#666; --li-border:#d8d8d8; }\n"
                + "    * { box-sizing:border-box; }\n"
                + "    body { margin:0; background:var(--li-bg); color:var(--li-text); font-family:'Segoe UI',Tahoma,Arial,sans-serif; min-height:100vh; }\n"
                + "    .topbar { height:58px; background:var(--li-surface); border-bottom:1px solid #e5e5e5; display:flex; align-items:center; justify-content:center; }\n"
                + "    .brand { width:min(92vw,1024px); display:flex; align-items:center; gap:10px; font-weight:700; }\n"
                + "    .logo { width:34px; height:34px; border-radius:6px; background:var(--li-blue); color:#fff; display:grid; place-items:center; font-weight:800; }\n"
                + "    .wrap { width:min(92vw,1024px); margin:34px auto; display:grid; grid-template-columns:1.15fr .85fr; gap:22px; }\n"
                + "    .hero, .card { background:var(--li-surface); border:1px solid var(--li-border); border-radius:12px; box-shadow:0 2px 10px rgba(0,0,0,.05); }\n"
                + "    .hero { padding:28px; }\n"
                + "    .hero h1 { margin:0 0 12px; font-size:34px; line-height:1.2; }\n"
                + "    .hero p { margin:0; color:var(--li-sub); font-size:16px; }\n"
                + "    .pulse { margin-top:18px; height:8px; border-radius:999px; background:linear-gradient(90deg,var(--li-blue),#378fe9,#7bb8ff); }\n"
                + "    .card { padding:22px; }\n"
                + "    .card h2 { margin:0 0 6px; font-size:22px; }\n"
                + "    .sub { margin:0 0 16px; color:var(--li-sub); font-size:14px; }\n"
                + "    .note { margin:0 0 12px; color:#3d3d3d; font-size:13px; background:#eef3f8; border:1px solid #dce6f1; padding:10px 11px; border-radius:8px; }\n"
                + "    label { display:block; font-weight:600; margin:12px 0 6px; }\n"
                + "    input { width:100%; padding:11px 12px; border:1px solid #b5b5b5; border-radius:8px; font-size:15px; }\n"
                + "    input:focus { outline:none; border-color:var(--li-blue); box-shadow:0 0 0 3px rgba(10,102,194,.14); }\n"
                + "    button { margin-top:16px; width:100%; border:0; border-radius:999px; background:var(--li-blue); color:#fff; font-weight:700; padding:12px; cursor:pointer; font-size:16px; }\n"
                + "    button:hover { filter:brightness(.96); }\n"
                + "    .warning { margin-bottom:12px; border:1px solid #e9a1a1; background:#fff2f2; color:#8d1d1d; padding:10px 12px; border-radius:8px; font-size:14px; }\n"
                + "    @media (max-width: 900px) { .wrap { grid-template-columns:1fr; margin-top:20px; } .hero h1 { font-size:28px; } }\n"
                + "  </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "  <header class=\"topbar\"><div class=\"brand\"><span class=\"logo\">in</span><span>Professional Access</span></div></header>\n"
                + "  <main class=\"wrap\">\n"
                + "    <section class=\"hero\">\n"
                + "      <h1>Welcome back</h1>\n"
                + "      <p>Sign in to continue to your dashboard. Only one authenticated session can stay active at a time.</p>\n"
                + "      <div class=\"pulse\"></div>\n"
                + "    </section>\n"
                + "    <section class=\"card\">\n"
                + "      <h2>Authorization</h2>\n"
                + "      <p class=\"sub\">Use your credentials to continue.</p>\n"
                + warningHtml
                + "      <p class=\"note\">Single-session protection is enabled for this application.</p>\n"
                + "      <form method=\"post\" action=\"/login\">\n"
                + "        <label for=\"username\">Username</label>\n"
                + "        <input id=\"username\" name=\"username\" type=\"text\" autocomplete=\"username\" required>\n"
                + "        <label for=\"password\">Password</label>\n"
                + "        <input id=\"password\" name=\"password\" type=\"password\" autocomplete=\"current-password\" required>\n"
                + "        <button type=\"submit\">Sign in</button>\n"
                + "      </form>\n"
                + "    </section>\n"
                + "  </main>\n"
                + "</body>\n"
                + "</html>\n";
    }

    /**
     * Builds the welcome page with live-rate and session-sync scripts.
     *
     * @param username authenticated username
     * @param joke initial joke content rendered on first page load
     * @return html for welcome page
     */
    @Override
    public String renderWelcomePage(String username, String joke) {
        return "<!doctype html>\n"
                + "<html lang=\"en\">\n"
                + "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Welcome</title>\n"
                + "<style>\n"
                + ":root{--li-blue:#0a66c2;--li-bg:#f3f2ef;--li-surface:#fff;--li-border:#d8d8d8;--li-text:#1f1f1f;--li-sub:#666;}*{box-sizing:border-box;}\n"
                + "body{margin:0;background:var(--li-bg);color:var(--li-text);font-family:'Segoe UI',Tahoma,Arial,sans-serif;min-height:100vh;}\n"
                + ".topbar{height:58px;background:#fff;border-bottom:1px solid #e5e5e5;display:flex;align-items:center;justify-content:center;}\n"
                + ".bar{width:min(94vw,1040px);display:flex;justify-content:space-between;align-items:center;}\n"
                + ".brand{display:flex;align-items:center;gap:10px;font-weight:700;}\n"
                + ".logo{width:34px;height:34px;border-radius:6px;background:var(--li-blue);color:#fff;display:grid;place-items:center;font-weight:800;}\n"
                + ".layout{width:min(94vw,1040px);margin:22px auto;display:grid;grid-template-columns:1fr 320px;gap:18px;}\n"
                + ".card{background:#fff;border:1px solid var(--li-border);border-radius:12px;box-shadow:0 2px 10px rgba(0,0,0,.05);}\n"
                + ".main{padding:20px 22px;}\n"
                + ".hero{padding:18px;border-radius:10px;background:linear-gradient(90deg,#0a66c2,#3b8ad8);color:#fff;margin-bottom:14px;}\n"
                + ".hero h1{margin:0 0 5px;font-size:24px;} .hero p{margin:0;opacity:.95;}\n"
                + ".section-title{font-weight:700;margin:12px 0 8px;} .msg{margin:0;color:#333;line-height:1.45;}\n"
                + ".joke{margin-top:8px;background:#f5f8fb;border:1px solid #d9e7f5;padding:10px 12px;border-radius:8px;color:#2a2a2a;}\n"
                + ".rates{margin-top:14px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:12px;}\n"
                + ".rate{font-size:14px;color:#2a2a2a;margin:4px 0;} .rate strong{font-size:16px;} .meta{font-size:12px;color:#667085;margin-top:8px;}\n"
                + ".side{padding:18px;} .chip{display:inline-block;background:#e8f3ff;color:#0a66c2;border:1px solid #cfe4fb;padding:4px 9px;border-radius:999px;font-size:12px;font-weight:600;margin-bottom:10px;}\n"
                + ".side p{margin:0 0 12px;color:#555;line-height:1.4;}\n"
                + ".logout button{width:100%;border:0;border-radius:999px;background:#0a66c2;color:#fff;font-weight:700;padding:11px;cursor:pointer;}\n"
                + ".logout button:hover{filter:brightness(.96);}\n"
                + "@media (max-width: 900px){.layout{grid-template-columns:1fr;}.hero h1{font-size:22px;}}\n"
                + "</style></head>\n"
                + "<body>\n"
                + "<header class=\"topbar\"><div class=\"bar\"><div class=\"brand\"><span class=\"logo\">in</span><span>Professional Access</span></div></div></header>\n"
                + "<main class=\"layout\">\n"
                + "  <section class=\"card main\">\n"
                + "    <div class=\"hero\"><h1>Hello " + WebDataUtils.escapeHtml(username) + "</h1><p>Your workspace is ready.</p></div>\n"
                + "    <p class=\"section-title\">Programming Joke</p>\n"
                + "    <p class=\"joke\" id=\"jokeText\">" + WebDataUtils.escapeHtml(joke) + "</p>\n"
                + "    <section class=\"rates\" id=\"ratesBox\">\n"
                + "      <div class=\"section-title\" style=\"margin-top:0;\">Investing.com Live Rates</div>\n"
                + "      <div class=\"rate\">USD/TRY: <strong id=\"usdTry\">Loading...</strong></div>\n"
                + "      <div class=\"rate\">SAUDI DINAR / TRY: <strong id=\"sarTry\">Loading...</strong></div>\n"
                + "      <div class=\"meta\" id=\"ratesMeta\">Refreshing every 5 seconds</div>\n"
                + "    </section>\n"
                + "  </section>\n"
                + "  <aside class=\"card side\">\n"
                + "    <span class=\"chip\">Session Active</span>\n"
                + "    <p>Logout from any tab will force all open tabs to return to login.</p>\n"
                + "    <form id=\"logoutForm\" class=\"logout\" method=\"post\" action=\"/logout\"><button type=\"submit\">Logout</button></form>\n"
                + "  </aside>\n"
                + "</main>\n"
                + "<script>\n"
                + "const LOGOUT_EVENT_KEY='globalLogoutAt';\n"
                + "function forceLoginRedirect(){window.location.href='/login';}\n"
                + "// Sync logout across tabs using localStorage broadcast.\n"
                + "window.addEventListener('storage',(e)=>{if(e.key===LOGOUT_EVENT_KEY){forceLoginRedirect();}});\n"
                + "const logoutForm=document.getElementById('logoutForm');\n"
                + "if(logoutForm){logoutForm.addEventListener('submit',()=>{try{localStorage.setItem(LOGOUT_EVENT_KEY,String(Date.now()));}catch(e){}});}\n"
                + "const usdTryEl=document.getElementById('usdTry');\n"
                + "const sarTryEl=document.getElementById('sarTry');\n"
                + "const jokeTextEl=document.getElementById('jokeText');\n"
                + "const ratesMetaEl=document.getElementById('ratesMeta');\n"
                + "async function refreshRates(){\n"
                + "  try{\n"
                + "    const r=await fetch('/rates',{credentials:'same-origin',cache:'no-store'});\n"
                + "    if(r.status===401){forceLoginRedirect();return;}\n"
                + "    const data=await r.json();\n"
                + "    usdTryEl.textContent=(typeof data.usdTry==='number')?data.usdTry.toFixed(4):'N/A';\n"
                + "    sarTryEl.textContent=(typeof data.sarTry==='number')?data.sarTry.toFixed(4):'N/A';\n"
                + "    if(typeof data.joke==='string' && data.joke.length>0){jokeTextEl.textContent=data.joke;}\n"
                + "    const stale=data.stale===true?' (cached)':'';\n"
                + "    const src=data.sourceDate?(' | Investing fetch: '+data.sourceDate):'';\n"
                + "    ratesMetaEl.textContent='Updated: '+new Date().toLocaleTimeString()+stale+src;\n"
                + "  }catch(e){ratesMetaEl.textContent='Rates unavailable, retrying...';}\n"
                + "}\n"
                + "refreshRates();\n"
                + "setInterval(refreshRates,5000);\n"
                + "// Polling protects against stale tabs where storage events are not delivered.\n"
                + "setInterval(async()=>{try{const r=await fetch('/session-status',{credentials:'same-origin',cache:'no-store'});if(r.status===401){forceLoginRedirect();}}catch(e){}},3000);\n"
                + "</script>\n"
                + "</body>\n"
                + "</html>\n";
    }
}
