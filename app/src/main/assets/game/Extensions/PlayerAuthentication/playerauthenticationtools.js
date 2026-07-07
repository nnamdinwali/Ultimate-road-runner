// Stubbed — no online login needed. Player is always "authenticated" so
// scores save immediately to localStorage via the patched leaderboardstools.js.
var gdjs;(function(gdjs){
  const auth={};
  auth.isAuthenticated=function(){return true;};
  auth.openAuthenticationWindow=function(){return{update:function(){return true;}};};
  auth.displayAuthenticationBanner=function(){};
  auth.removeAuthenticationBanner=function(){};
  auth.getAuthorizationHeader=function(){return'';};
  auth.getUsernameFromStorage=function(){return'Player';};
  auth.logout=function(){};
  auth.isAuthenticationWindowOpen=function(){return false;};
  auth.hasAuthenticationErrored=function(){return false;};
  auth.hasPlayerBeenAuthenticated=function(){return true;};
  gdjs.playerAuthentication=auth;
})(gdjs||(gdjs={}));
