# TSW Interface (Fabric 1.21.11)

واجهة إنفنتوري زجاجية بدل واجهة ماين كرافت الأساسية. تفتح بزر E تلقائياً.

## البناء عن طريق GitHub
1. سوّ Repository جديد وارفع كل محتوى هذا المجلد (مع مجلد `.github`).
2. روح تبويب **Actions** ← **Build TSW Interface** ← انتظر يخلص.
3. نزّل الـ Artifact اللي اسمه `TSW-Interface-jar` وفكه، فيه `TSW-Interface-1.0.0.jar`.
4. حطه في مجلد `mods` (مع Fabric Loader 0.18+ و Fabric API 0.141.1+1.21.11).

## تعديلات سريعة
- اللوقو: استبدل `src/main/resources/assets/tsw_interface/textures/gui/logo.png` بلوقو ND حقك (180x132 بكسل شفاف).
- الـ HUD (الهوتبار والأنابيب وشريط الخبرة): الملف `TswHud.java`.
- الألوان والأحجام: الثوابت في أول `TswInventoryScreen.java` (مساحة التصميم 1000x560).
