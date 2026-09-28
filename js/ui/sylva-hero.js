/* SylvaHero (living-green) — vanilla bridge for static site
   Mirrors LandingPages.tsx SylvaHero + pageTypography behavior without React.
   The authored document is served byte-exact at /landing-pages/inner-green-3d.html
   and typography overrides are appended exactly as usePageTypography would emit.
*/
(function(){
  var PRIMARY = "#ffffff";
  // SYLVA_TYPOGRAPHY css generator (from pageRecipes.ts) — byte-exact logic
  function withAlpha(hex, alpha){
    var r = parseInt(hex.slice(1,3),16), g=parseInt(hex.slice(3,5),16), b=parseInt(hex.slice(5,7),16);
    return "rgba("+r+", "+g+", "+b+", "+alpha+")";
  }
  function unit(v){ return "calc("+Number(v.toFixed(3))+" * var(--u))"; }
  function sylvaCss(type){
    return ":root {\n  --ink: "+type.primary+";\n  --ink-soft: "+withAlpha(type.primary,0.62)+";\n  --ink-faint: "+withAlpha(type.primary,0.44)+";\n}\nbody { font-family: "+type.body+"; font-weight: "+type.bodyWeight+"; }\n.headline, .ghost {\n  font-family: "+type.heading+";\n}\n.headline {\n  font-weight: "+type.headingWeight+";\n  font-size: "+unit(type.headingSize)+";\n  line-height: "+unit((type.headingSize*65)/63)+";\n  letter-spacing: "+type.headingLetterSpacing+"em;\n}\n.lede {\n  font-weight: "+type.bodyWeight+";\n  font-size: "+unit(type.bodySize)+";\n  line-height: "+unit((type.bodySize*22)/16.5)+";\n}\n@media (max-width: 900px) {\n  .headline {\n    font-size: "+unit((type.headingSize*62)/63)+";\n    line-height: "+unit((type.headingSize*66)/63)+";\n  }\n  .lede {\n    font-size: "+unit((type.bodySize*19)/16.5)+";\n    line-height: "+unit((type.bodySize*27)/16.5)+";\n  }\n}\n";
  }
  // Configured props from prompt
  var props = {
    headingFont: "lexend",
    bodyFont: "lexend",
    headingWeight: "300",
    bodyWeight: "300",
    primaryColor: "#ffffff",
    headingSize: 63,
    bodySize: 16.5,
    headingLetterSpacing: -0.006
  };
  var LEXEND_STACK = "'Lexend', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif";
  var type = {
    heading: LEXEND_STACK,
    body: LEXEND_STACK,
    headingWeight: props.headingWeight,
    bodyWeight: props.bodyWeight,
    primary: props.primaryColor,
    headingSize: props.headingSize,
    bodySize: props.bodySize,
    headingLetterSpacing: props.headingLetterSpacing
  };
  var css = sylvaCss(type);
  window.__sylvaHeroLivingGreenCustomization = { css: css };

  function applyCustomization(frame){
    if(!frame || !frame.contentDocument) return;
    var doc = frame.contentDocument;
    if(!doc.head) return;
    var id = "threeui-page-typography";
    var style = doc.getElementById(id);
    if(!style){
      style = doc.createElement("style");
      style.id = id;
      doc.head.appendChild(style);
    }
    style.textContent = css;
    // ensure last in head
    doc.head.appendChild(style);
  }
  window.applySylvaHeroCustomization = applyCustomization;

  document.addEventListener("DOMContentLoaded", function(){
    var frames = document.querySelectorAll("iframe[data-sylva-hero]");
    frames.forEach(function(frame){
      frame.addEventListener("load", function(){ applyCustomization(frame); });
      // if already loaded
      if(frame.contentDocument && frame.contentDocument.readyState === "complete"){
        applyCustomization(frame);
      }
    });
  });
})();
