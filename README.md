Use our code to save yourself time on cross-platform, cross-device and cross OS version development and testing
# android_module_News
News widget is intended for displaying of news feed, defined in format of RSS-feed or Atom. Also provides the ability to display news, added manually at the manage content panel.

General features:

- viewing of news list;
- viewing of a page with detailed view of each news;
- redirect via external link (if link exists in detailed view of news).

Tags:

- title - widget name. Title is being displayed on navigation panel when widget is launched.
- colorskin - this is root tag to set up color scheme. Contains 5 elements (color[1-5]). Each widget may set colors for elements of the interface using the color scheme in any order, however generally color1 - background color, color3 - titles color, color4 - font color, color5 - date or price color.
- url - RSS-feed URL link.

Example:


    <data>
      <title><![CDATA[ My RSS page ]]></title>
      <url><![CDATA[ http://news.google.nl/?output=rss ]]></url>
      <colorskin>
        <color1><![CDATA[ #23660f ]]></color1>
        <color2><![CDATA[ #fbff94 ]]></color2>
        <color3><![CDATA[ #b7ffa2 ]]></color3>
        <color4><![CDATA[ #ffffff ]]></color4>
        <color5><![CDATA[ #fbff94 ]]></color5>
      </colorskin>
    </data>
