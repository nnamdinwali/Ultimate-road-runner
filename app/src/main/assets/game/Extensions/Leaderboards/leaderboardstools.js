// Local leaderboard — stores top scores in device localStorage.
// Replaces GDevelop's online leaderboard so no login/internet needed.
var gdjs;(function(gdjs){
  if(!gdjs.evtTools)gdjs.evtTools={};
  const lb={};
  const KEY='URR_LB_';
  const MAX=10;
  let _saving={},_justClosed=false,_open=false;

  function load(id){try{const d=localStorage.getItem(KEY+id);return d?JSON.parse(d):[]}catch(e){return[]}}
  function save(id,arr){try{localStorage.setItem(KEY+id,JSON.stringify(arr))}catch(e){}}

  lb.savePlayerScore=lb.saveConnectedPlayerScore=function(scene,id,score){
    _saving[id]=true;
    const arr=load(id);
    arr.push({score:Math.round(score),date:new Date().toLocaleDateString()});
    arr.sort((a,b)=>b.score-a.score);
    if(arr.length>MAX)arr.length=MAX;
    save(id,arr);
    setTimeout(()=>{_saving[id]=false;},120);
  };

  lb.isSaving=function(id){return!!_saving[id];};
  lb.hasBeenSaved=function(){return true;};
  lb.hasSavingErrored=function(){return false;};
  lb.getLastSaveError=function(){return'';};
  lb.setPreferSendConnectedPlayerScore=function(){};
  lb.isLeaderboardViewLoading=function(){return false;};
  lb.isLeaderboardViewLoaded=function(){return true;};
  lb.isLeaderboardViewErrored=function(){return false;};
  lb.hasPlayerJustClosedLeaderboardView=function(){if(_justClosed){_justClosed=false;return true;}return false;};
  lb.closeLeaderboardView=function(){const el=document.getElementById('_lb_ov');if(el)el.remove();_open=false;_justClosed=true;};
  lb.formatPlayerName=function(n){return n;};

  lb.displayLeaderboard=function(scene,id){
    _open=true;_justClosed=false;
    const existing=document.getElementById('_lb_ov');if(existing)existing.remove();
    const scores=load(id);

    const ov=document.createElement('div');
    ov.id='_lb_ov';
    ov.style.cssText='position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.92);z-index:2147483647;display:flex;flex-direction:column;align-items:center;padding:40px 16px 20px;box-sizing:border-box;font-family:sans-serif;color:#fff;overflow-y:auto;';

    const title=document.createElement('div');
    title.innerText='LEADERBOARD';
    title.style.cssText='font-size:24px;font-weight:900;letter-spacing:3px;color:#FFD700;margin-bottom:4px;';
    ov.appendChild(title);

    const sub=document.createElement('div');
    sub.innerText='Best scores on this device';
    sub.style.cssText='font-size:12px;color:#888;margin-bottom:24px;';
    ov.appendChild(sub);

    const list=document.createElement('div');
    list.style.cssText='width:100%;max-width:340px;';

    if(scores.length===0){
      const empty=document.createElement('div');
      empty.innerText='No scores yet — play a game first!';
      empty.style.cssText='text-align:center;color:#666;font-size:14px;margin-top:30px;';
      list.appendChild(empty);
    } else {
      scores.forEach(function(e,i){
        const row=document.createElement('div');
        const top=i===0;
        row.style.cssText='display:flex;justify-content:space-between;align-items:center;padding:11px 16px;margin-bottom:8px;border-radius:10px;background:'+(top?'rgba(255,215,0,0.13)':'rgba(255,255,255,0.06)')+';'+(top?'border:1px solid rgba(255,215,0,0.35)':'');

        const medal=document.createElement('span');
        medal.innerText=i===0?'🥇':i===1?'🥈':i===2?'🥉':'#'+(i+1);
        medal.style.cssText='font-size:20px;width:36px;flex-shrink:0;';

        const sc=document.createElement('span');
        sc.innerText=e.score.toLocaleString();
        sc.style.cssText='font-size:22px;font-weight:bold;color:'+(top?'#FFD700':'#fff')+';flex:1;text-align:center;';

        const dt=document.createElement('span');
        dt.innerText=e.date||'';
        dt.style.cssText='font-size:11px;color:#666;width:70px;text-align:right;flex-shrink:0;';

        row.appendChild(medal);row.appendChild(sc);row.appendChild(dt);
        list.appendChild(row);
      });
    }
    ov.appendChild(list);

    const btn=document.createElement('button');
    btn.innerText='CLOSE';
    btn.style.cssText='margin-top:30px;padding:13px 50px;background:#e63946;color:#fff;border:none;border-radius:30px;font-size:16px;font-weight:bold;letter-spacing:1px;cursor:pointer;touch-action:manipulation;flex-shrink:0;';
    function closeOv(ev){ev.stopImmediatePropagation();ev.preventDefault();ov.remove();_open=false;_justClosed=true;}
    btn.addEventListener('touchstart',closeOv,{capture:true,passive:false});
    btn.addEventListener('click',closeOv,{capture:true});
    ov.appendChild(btn);

    document.body.appendChild(ov);
  };

  gdjs.evtTools.leaderboards=lb;
})(gdjs||(gdjs={}));
