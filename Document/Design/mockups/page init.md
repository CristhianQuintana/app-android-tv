<!DOCTYPE html>

<html class="dark" lang="es"><head>
<meta charset="utf-8"/>
<meta content="width=device-width, initial-scale=1.0" name="viewport"/>
<title>IPTV Cinema - Smart TV Experience</title>
<!-- Google Fonts: Outfit & Plus Jakarta Sans -->
<link href="https://fonts.googleapis.com" rel="preconnect"/>
<link crossorigin="" href="https://fonts.gstatic.com" rel="preconnect"/>
<link href="https://fonts.googleapis.com/css2?family=Outfit:wght@500;600;700;800&amp;family=Plus+Jakarta+Sans:wght@400;500;600;700&amp;display=swap" rel="stylesheet"/>
<!-- Material Symbols Outlined -->
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&amp;display=swap" rel="stylesheet"/>
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&amp;display=swap" rel="stylesheet"/>
<!-- Tailwind CSS v3 with plugins -->
<script src="https://cdn.tailwindcss.com?plugins=forms,container-queries"></script>
<script id="tailwind-config">
    tailwind.config = {
      darkMode: "class",
      theme: {
        extend: {
          "colors": {
            "on-secondary-fixed-variant": "#004c69",
            "on-primary-container": "#003751",
            "on-surface": "#e1e2ec",
            "on-tertiary-fixed": "#00174b",
            "inverse-on-surface": "#2d3038",
            "surface-bright": "#363941",
            "background": "#10131a",
            "inverse-primary": "#006591",
            "on-primary": "#00344d",
            "surface-container-high": "#272a32",
            "on-background": "#e1e2ec",
            "surface-tint": "#89ceff",
            "primary-fixed-dim": "#89ceff",
            "tertiary-container": "#7397ff",
            "error-container": "#93000a",
            "inverse-surface": "#e1e2ec",
            "primary": "#89ceff",
            "on-error": "#690005",
            "on-tertiary": "#002a78",
            "on-surface-variant": "#bec8d2",
            "tertiary": "#b4c5ff",
            "on-secondary-container": "#00374d",
            "tertiary-fixed-dim": "#b4c5ff",
            "outline-variant": "#3e4850",
            "surface": "#10131a",
            "on-error-container": "#ffdad6",
            "secondary-fixed": "#c4e7ff",
            "on-primary-fixed-variant": "#004c6e",
            "primary-container": "#0ea5e9",
            "on-primary-fixed": "#001e2f",
            "error": "#ffb4ab",
            "surface-container-highest": "#32353d",
            "surface-container-lowest": "#0b0e15",
            "on-tertiary-container": "#002c7e",
            "surface-variant": "#32353d",
            "on-secondary-fixed": "#001e2c",
            "surface-dim": "#10131a",
            "primary-fixed": "#c9e6ff",
            "secondary-container": "#00a6e0",
            "surface-container-low": "#191b23",
            "outline": "#88929b",
            "secondary-fixed-dim": "#7bd0ff",
            "secondary": "#7bd0ff",
            "tertiary-fixed": "#dbe1ff",
            "surface-container": "#1d1f27",
            "on-secondary": "#00354a",
            "on-tertiary-fixed-variant": "#003ea8"
          },
          "borderRadius": {
            "DEFAULT": "0.25rem",
            "lg": "0.5rem",
            "xl": "0.75rem",
            "full": "9999px"
          },
          "spacing": {
            "gutter": "1.5rem",
            "margin-safe": "4rem",
            "space-lg": "2rem",
            "space-xl": "3rem",
            "space-xs": "0.5rem",
            "space-md": "1.25rem",
            "space-sm": "0.75rem",
            "margin": "3rem",
            "gutter-tv": "2rem"
          },
          "fontFamily": {
            "body-xl": ["Plus Jakarta Sans"],
            "headline-sm": ["Outfit"],
            "label-md": ["Outfit"],
            "headline-xl": ["Outfit"],
            "headline-lg": ["Outfit"],
            "label-lg": ["Outfit"],
            "label-sm": ["Outfit"],
            "body-md": ["Plus Jakarta Sans"],
            "headline-md": ["Outfit"],
            "body-lg": ["Plus Jakarta Sans"]
          },
          "fontSize": {
            "body-xl": ["24px", { "lineHeight": "34px", "letterSpacing": "0em", "fontWeight": "500" }],
            "headline-sm": ["28px", { "lineHeight": "36px", "letterSpacing": "0em", "fontWeight": "600" }],
            "label-md": ["16px", { "lineHeight": "22px", "letterSpacing": "0.06em", "fontWeight": "600" }],
            "headline-xl": ["56px", { "lineHeight": "64px", "letterSpacing": "-0.02em", "fontWeight": "800" }],
            "headline-lg": ["44px", { "lineHeight": "52px", "letterSpacing": "-0.01em", "fontWeight": "700" }],
            "label-lg": ["20px", { "lineHeight": "26px", "letterSpacing": "0.04em", "fontWeight": "600" }],
            "label-sm": ["14px", { "lineHeight": "18px", "letterSpacing": "0.08em", "fontWeight": "700" }],
            "body-md": ["18px", { "lineHeight": "26px", "letterSpacing": "0.01em", "fontWeight": "400" }],
            "headline-md": ["36px", { "lineHeight": "44px", "letterSpacing": "0em", "fontWeight": "700" }],
            "body-lg": ["20px", { "lineHeight": "30px", "letterSpacing": "0em", "fontWeight": "400" }]
          }
        }
      }
    }
  </script>
<style>
    .material-symbols-outlined {
      font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
      display: inline-block;
      vertical-align: middle;
      line-height: 1;
    }
    .custom-scrollbar::-webkit-scrollbar {
      display: none;
    }
    .custom-scrollbar {
      -ms-overflow-style: none;
      scrollbar-width: none;
    }
    /* Ambient glow for the focused element */
    .focus-tv-active {
      box-shadow: 0 0 0 3px #0ea5e9, 0 16px 36px rgba(14, 165, 233, 0.45);
    }
  </style>
</head>
<body class="bg-background text-on-surface antialiased overflow-x-hidden min-h-screen select-none font-body-md text-body-md">
<!-- ================= TOP APP BAR (Shared Component JSON) ================= -->
<header class="fixed top-0 left-0 w-full z-40 flex justify-between items-center px-margin-safe pt-space-md pb-space-lg bg-gradient-to-b from-surface-container-lowest to-transparent pointer-events-none">
<!-- Brand Logo / Product Name -->
<div class="flex items-center gap-space-sm pointer-events-auto pl-24">
<span class="text-headline-sm font-headline-sm font-bold text-primary dark:text-primary tracking-tight">IPTV Cinema</span>
<span class="bg-surface-container-high/80 text-primary border border-outline-variant/40 px-space-xs py-0.5 rounded text-label-sm font-label-sm uppercase tracking-wider">PREMIUM 4K</span>
</div>
<!-- Right Cluster: Network, Time, User Profile -->
<div class="flex items-center gap-space-lg pointer-events-auto">
<!-- Wi-Fi Status -->
<div class="flex items-center gap-1.5 text-on-surface-variant dark:text-on-surface-variant text-label-md font-label-md">
<span class="material-symbols-outlined text-primary text-2xl" data-icon="wifi">wifi</span>
<span class="text-label-sm font-label-sm">5G ULTRA</span>
</div>
<!-- Schedule / Clock -->
<div class="flex items-center gap-1.5 text-on-surface text-label-md font-label-md">
<span class="material-symbols-outlined text-on-surface-variant text-xl" data-icon="schedule">schedule</span>
<span class="font-headline-sm text-headline-sm tracking-wide text-on-surface">20:45</span>
</div>
<!-- User Profile Avatar Pill -->
<div class="flex items-center gap-space-sm bg-surface-container-high/60 backdrop-blur-md px-space-sm py-1.5 rounded-full border border-outline-variant/30">
<div class="relative w-9 h-9 rounded-full overflow-hidden ring-2 ring-primary">
<img class="w-full h-full object-cover" data-alt="A sophisticated cinematic headshot portrait of an adult in high-definition low-key studio lighting with deep obsidian and rich cyan rim light reflections, matching a futuristic home entertainment dark mode interface aesthetic with impeccable 4K quality." src="https://lh3.googleusercontent.com/aida-public/AB6AXuD5gtl4-QxhpwBkaBsuqJnv5P8vLdSZxxHaMgoAKzkCzTU8ZOJhrU7IBqaLLQBJTX9hVSnIV9WFnMZq2arbobG3m4bIyLElv8zl7UriRF5dfemwI4RFfILtxnlTxzxs3z8ov2Zep8qhIuckYIOLYHp4KevzB0841CElkc9zgtVVlJNpN4r9K7-X_ig6CfgP5SGVAYOo-8VBMgt6xKgM38BxuBicuN7wsrmGumq2n87hv5inCRXg8lr09w"/>
</div>
<span class="text-label-sm font-label-sm font-semibold text-on-surface pr-1">Familia VIP</span>
</div>
</div>
</header>
<!-- ================= SIDE NAVIGATION RAIL (Shared Component JSON) ================= -->
<aside class="fixed left-0 top-0 h-full z-50 flex flex-col justify-between py-margin-safe pl-space-md pr-space-md bg-surface-container-lowest/80 dark:bg-surface-container-lowest/80 backdrop-blur-xl border-r border-outline-variant/30 shadow-2xl w-24 hover:w-72 transition-all duration-300 group">
<!-- Top Nav Icon Stack -->
<div class="flex flex-col gap-space-md">
<!-- App Mark -->
<div class="flex items-center gap-space-sm px-space-sm py-space-xs mb-space-sm">
<div class="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-container to-secondary flex items-center justify-center text-on-primary shadow-lg shadow-primary/20">
<span class="material-symbols-outlined text-2xl font-bold" data-icon="live_tv">live_tv</span>
</div>
<span class="opacity-0 group-hover:opacity-100 transition-opacity duration-200 text-headline-sm font-headline-sm font-extrabold text-primary whitespace-nowrap overflow-hidden">Cinema TV</span>
</div>
<!-- Nav Items -->
<nav class="flex flex-col gap-2.5">
<!-- Item 1: Inicio (ACTIVE) -->
<a class="flex items-center gap-space-sm bg-primary-container/20 dark:bg-primary-container/20 text-primary dark:text-primary rounded-xl px-space-md py-space-sm ring-2 ring-primary focus:scale-105 focus:bg-primary-container/30 focus:text-primary focus:ring-2 focus:ring-primary focus:outline-none transition-transform duration-180" href="#inicio">
<span class="material-symbols-outlined text-2xl" data-icon="home" style="font-variation-settings: 'FILL' 1;">home</span>
<span class="opacity-0 group-hover:opacity-100 transition-opacity duration-200 text-label-lg font-label-lg font-bold whitespace-nowrap">Inicio</span>
</a>
<!-- Item 2: TV en Vivo -->
<a class="flex items-center gap-space-sm text-on-surface-variant dark:text-on-surface-variant rounded-xl px-space-md py-space-sm hover:text-on-surface dark:hover:text-on-surface hover:bg-surface-container-high/40 focus:scale-105 focus:bg-primary-container/30 focus:text-primary focus:ring-2 focus:ring-primary focus:outline-none transition-transform duration-180" href="#live">
<span class="material-symbols-outlined text-2xl" data-icon="live_tv">live_tv</span>
<span class="opacity-0 group-hover:opacity-100 transition-opacity duration-200 text-label-lg font-label-lg whitespace-nowrap">TV en Vivo</span>
</a>
<!-- Item 3: Películas -->
<a class="flex items-center gap-space-sm text-on-surface-variant dark:text-on-surface-variant rounded-xl px-space-md py-space-sm hover:text-on-surface dark:hover:text-on-surface hover:bg-surface-container-high/40 focus:scale-105 focus:bg-primary-container/30 focus:text-primary focus:ring-2 focus:ring-primary focus:outline-none transition-transform duration-180" href="#peliculas">
<span class="material-symbols-outlined text-2xl" data-icon="movie">movie</span>
<span class="opacity-0 group-hover:opacity-100 transition-opacity duration-200 text-label-lg font-label-lg whitespace-nowrap">Películas</span>
</a>
<!-- Item 4: Series -->
<a class="flex items-center gap-space-sm text-on-surface-variant dark:text-on-surface-variant rounded-xl px-space-md py-space-sm hover:text-on-surface dark:hover:text-on-surface hover:bg-surface-container-high/40 focus:scale-105 focus:bg-primary-container/30 focus:text-primary focus:ring-2 focus:ring-primary focus:outline-none transition-transform duration-180" href="#series">
<span class="material-symbols-outlined text-2xl" data-icon="tv">tv</span>
<span class="opacity-0 group-hover:opacity-100 transition-opacity duration-200 text-label-lg font-label-lg whitespace-nowrap">Series</span>
</a>
<!-- Item 5: Categorías -->
<a class="flex items-center gap-space-sm text-on-surface-variant dark:text-on-surface-variant rounded-xl px-space-md py-space-sm hover:text-on-surface dark:hover:text-on-surface hover:bg-surface-container-high/40 focus:scale-105 focus:bg-primary-container/30 focus:text-primary focus:ring-2 focus:ring-primary focus:outline-none transition-transform duration-180" href="#categorias">
<span class="material-symbols-outlined text-2xl" data-icon="grid_view">grid_view</span>
<span class="opacity-0 group-hover:opacity-100 transition-opacity duration-200 text-label-lg font-label-lg whitespace-nowrap">Categorías</span>
</a>
</nav>
</div>
<!-- Bottom Nav Footer Tabs -->
<div class="flex flex-col gap-2 pt-4 border-t border-outline-variant/30">
<!-- Search -->
<a class="flex items-center gap-space-sm text-on-surface-variant dark:text-on-surface-variant rounded-xl px-space-md py-space-sm hover:text-on-surface dark:hover:text-on-surface hover:bg-surface-container-high/40 focus:scale-105 focus:bg-primary-container/30 focus:text-primary focus:ring-2 focus:ring-primary focus:outline-none transition-transform duration-180" href="#search">
<span class="material-symbols-outlined text-2xl" data-icon="search">search</span>
<span class="opacity-0 group-hover:opacity-100 transition-opacity duration-200 text-label-lg font-label-lg whitespace-nowrap">Búsqueda</span>
</a>
<!-- Profile -->
<a class="flex items-center gap-space-sm text-on-surface-variant dark:text-on-surface-variant rounded-xl px-space-md py-space-sm hover:text-on-surface dark:hover:text-on-surface hover:bg-surface-container-high/40 focus:scale-105 focus:bg-primary-container/30 focus:text-primary focus:ring-2 focus:ring-primary focus:outline-none transition-transform duration-180" href="#profile">
<span class="material-symbols-outlined text-2xl" data-icon="account_circle">account_circle</span>
<span class="opacity-0 group-hover:opacity-100 transition-opacity duration-200 text-label-lg font-label-lg whitespace-nowrap">Mi Perfil</span>
</a>
</div>
</aside>
<!-- ================= MAIN CANVAS CONTAINER ================= -->
<main class="pl-28 pr-margin-safe pb-24 relative overflow-hidden">
<!-- ================= HERO BILLBOARD CINEMATOGRÁFICO ================= -->
<section class="relative w-full h-[620px] rounded-3xl overflow-hidden mt-6 mb-space-xl border border-outline-variant/20 shadow-2xl bg-surface-container-lowest">
<!-- 4K Hero Cinematic Background Artwork -->
<div class="absolute inset-0 bg-cover bg-center scale-100 hover:scale-105 transition-transform duration-1000" data-alt="An awe-inspiring 4K cinematic science-fiction scene featuring an interstellar deep space exploratory vessel approaching a magnificent bioluminescent nebulous planetary rift. The dramatic lighting casts vibrant electric cyan and midnight navy rim-glows across sleek starship metal and cosmic atmospheric dust, blending naturally with dark obsidian shadows in a low-key 10-foot television display experience." style="background-image: url('https://lh3.googleusercontent.com/aida-public/AB6AXuBTBDI0X-fuEEKdcjMl9g2lzREJp-oqzDrT28ViPtHQ7seKB76blyEVAiLxEqlRwsmPpO5GImbt9I0BhBsobbX-7xA7cCliEXE53spFg9Qn6N9_DKeenh5l2_6LuaPOOpIQmwPjY8Mn8U8g9mmJq2GRkr2JmhGIE80K2VSp98CMqkMvD_NbIBY1P4vLyvFO6lhtKS3IHW35dhVV5VjXhkTXrzCJADcTvY2bK3X_8JF4OT1GsvQ48QUd0g')"></div>
<!-- Scrim Overlays (Bottom and Left gradient to solid background) -->
<div class="absolute inset-0 bg-gradient-to-t from-background via-background/70 to-transparent"></div>
<div class="absolute inset-0 bg-gradient-to-r from-background via-background/85 to-transparent w-3/4"></div>
<div class="absolute inset-0 bg-radial-at-t from-transparent via-transparent to-background/50"></div>
<!-- Hero Metadata Container -->
<div class="absolute bottom-10 left-12 max-w-3xl z-10 flex flex-col gap-space-sm">
<!-- Quality & Recognition Badges Cluster -->
<div class="flex items-center gap-2.5 flex-wrap">
<span class="bg-primary/20 text-primary border border-primary/40 px-2.5 py-0.5 rounded text-label-sm font-label-sm font-bold tracking-widest uppercase">ESTRENO EXCLUSIVO</span>
<span class="bg-surface-container-high/90 text-on-surface border border-outline-variant/40 px-2 py-0.5 rounded text-label-sm font-label-sm font-bold">4K UHD</span>
<span class="bg-surface-container-high/90 text-on-surface border border-outline-variant/40 px-2 py-0.5 rounded text-label-sm font-label-sm font-bold">HDR10+</span>
<span class="bg-surface-container-high/90 text-on-surface border border-outline-variant/40 px-2 py-0.5 rounded text-label-sm font-label-sm font-bold">Dolby Atmos</span>
<span class="text-secondary font-label-md font-label-md font-bold flex items-center gap-1">
<span class="material-symbols-outlined text-lg text-primary" data-icon="thumb_up" style="font-variation-settings: 'FILL' 1;">thumb_up</span>
            98% Coincidencia
          </span>
<span class="text-on-surface-variant font-label-sm font-label-sm border border-outline-variant/50 px-1.5 py-0.5 rounded">PG-13</span>
<span class="text-on-surface-variant font-label-sm font-label-sm">2h 38m</span>
</div>
<!-- Hero Title -->
<h1 class="text-headline-xl font-headline-xl text-on-surface font-extrabold tracking-tight drop-shadow-md">
          CHRONOS: <span class="text-transparent bg-clip-text bg-gradient-to-r from-primary via-secondary to-primary-fixed">EL HORIZONTE</span>
</h1>
<!-- Hero Synopsis -->
<p class="text-body-lg font-body-lg text-on-surface-variant max-w-2xl line-clamp-2 leading-relaxed">
          En el borde exterior de una galaxia moribunda, una tripulación solitaria arriesga las últimas reservas temporales para interceptar una señal alienígena que podría revertir el colapso cuántico de la Tierra.
        </p>
<!-- Focused Remote Action Buttons (TV D-Pad Engine) -->
<div class="flex items-center gap-space-md pt-space-xs">
<!-- PRIMARY BUTTON: ACTIVELY FOCUSED BY D-PAD -->
<button class="focus-tv-active transform scale-105 bg-gradient-to-r from-primary-container to-secondary-container text-on-primary-fixed font-headline-sm text-label-lg font-bold px-8 py-3.5 rounded-full flex items-center gap-3 transition-all duration-200 ring-4 ring-primary cursor-pointer shadow-lg shadow-primary-container/40">
<span class="material-symbols-outlined text-3xl font-extrabold text-on-primary-fixed" data-icon="play_arrow" style="font-variation-settings: 'FILL' 1;">play_arrow</span>
<span>Reproducir Ahora</span>
</button>
<!-- SECONDARY BUTTON: MI LISTA -->
<button class="bg-surface-container-high/70 hover:bg-surface-container-highest/90 border border-outline-variant/40 text-on-surface font-label-lg text-label-lg font-semibold px-6 py-3.5 rounded-full flex items-center gap-2.5 backdrop-blur-md transition-all duration-180 hover:scale-105 hover:ring-2 hover:ring-primary focus:ring-2 focus:ring-primary cursor-pointer">
<span class="material-symbols-outlined text-2xl" data-icon="bookmark_add">bookmark_add</span>
<span>Mi Lista</span>
</button>
<!-- TERTIARY BUTTON: MÁS INFORMACIÓN -->
<button class="bg-surface-container-high/50 hover:bg-surface-container-highest/90 border border-outline-variant/30 text-on-surface-variant hover:text-on-surface font-label-lg text-label-lg font-medium px-5 py-3.5 rounded-full flex items-center gap-2 backdrop-blur-md transition-all duration-180 hover:scale-105 cursor-pointer">
<span class="material-symbols-outlined text-2xl" data-icon="info">info</span>
<span>Detalles</span>
</button>
</div>
</div>
</section>
<!-- ================= CARRILES HORIZONTALES (MEDIA SWIMLANES) ================= -->
<div class="flex flex-col gap-space-xl">
<!-- ================= CARRIL 1: CONTINUAR VIENDO (16:9 Tiles) ================= -->
<section class="flex flex-col gap-space-sm">
<div class="flex items-center justify-between">
<div class="flex items-center gap-space-xs">
<span class="material-symbols-outlined text-primary text-2xl" data-icon="play_circle">play_circle</span>
<h2 class="text-headline-sm font-headline-sm text-on-surface font-bold tracking-tight">Continuar Viendo</h2>
</div>
<span class="text-label-md font-label-md text-on-surface-variant hover:text-primary transition-colors cursor-pointer flex items-center gap-1">
            Ver todo <span class="material-symbols-outlined text-lg" data-icon="arrow_forward_ios">arrow_forward_ios</span>
</span>
</div>
<!-- Horizontal Swimlane -->
<div class="flex gap-gutter-tv overflow-x-auto custom-scrollbar pt-2 pb-4">
<!-- Card 1 -->
<div class="flex-shrink-0 w-80 group cursor-pointer transition-transform duration-200 hover:scale-105 focus:scale-105">
<div class="relative w-full aspect-video rounded-2xl overflow-hidden bg-surface-container-low border border-outline-variant/30 group-hover:border-primary group-focus:border-primary group-hover:shadow-lg group-hover:shadow-primary/20 transition-all">
<img class="w-full h-full object-cover" data-alt="A vivid still frame from a cyberpunk detective mystery series showing rain-slicked neon street reflections in midnight blue and intense magenta tones, featuring a sharp futuristic vehicle speeding away into the misty shadows of a high-tech metropolis." src="https://lh3.googleusercontent.com/aida-public/AB6AXuAAvsKhQxJ1L-2QjtWRKnYjc7EfgHTcIg8MjjOgJD7HXEgQDcnjt9bRAanTLf_1tltMzzDUQvlKQxaPyQVk0eOElpiDJ4jcf1Zyv6ZzlIDrK6k8nCCMvVCXbbgtxVy6ZXI4mS6vdnH8VY7wL7rXJJ38ep9o79QIETPA2-TDetB-ksa2pl7tXrWLiCRro-UNr7M9jvSwHS_JjxhRrUw2nLH6old2z92ehteUNAHJz_AF3ptdIRHFveUs5A"/>
<!-- Overlay Pill -->
<span class="absolute top-3 left-3 bg-surface-container-lowest/80 backdrop-blur-md text-on-surface border border-outline-variant/30 px-2 py-0.5 rounded text-label-sm font-label-sm font-semibold">T2:E4</span>
<!-- Play overlay icon on hover -->
<div class="absolute inset-0 bg-background/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
<div class="w-12 h-12 rounded-full bg-primary-container text-on-primary flex items-center justify-center shadow-lg">
<span class="material-symbols-outlined text-3xl font-bold" data-icon="play_arrow" style="font-variation-settings: 'FILL' 1;">play_arrow</span>
</div>
</div>
<!-- Progress Bar -->
<div class="absolute bottom-0 left-0 w-full h-1.5 bg-surface-container-highest">
<div class="h-full bg-primary-container shadow-sm shadow-primary" style="width: 68%;"></div>
</div>
</div>
<div class="mt-2.5 flex justify-between items-baseline">
<h3 class="text-label-lg font-label-lg font-bold text-on-surface truncate">Sector 9: Protocolo Cero</h3>
<span class="text-label-sm font-label-sm text-primary flex-shrink-0">18 min rest.</span>
</div>
<p class="text-body-md font-body-md text-on-surface-variant text-sm truncate">Episodio 4: Sombras en la Red Neural</p>
</div>
<!-- Card 2 -->
<div class="flex-shrink-0 w-80 group cursor-pointer transition-transform duration-200 hover:scale-105 focus:scale-105">
<div class="relative w-full aspect-video rounded-2xl overflow-hidden bg-surface-container-low border border-outline-variant/30 group-hover:border-primary group-focus:border-primary group-hover:shadow-lg group-hover:shadow-primary/20 transition-all">
<img class="w-full h-full object-cover" data-alt="A cinematic documentary still showing an aerial perspective of majestic glacial fjords under the polar night, with vibrant emerald and turquoise aurora borealis illuminating dark icy oceanic waters in pristine natural HDR clarity." src="https://lh3.googleusercontent.com/aida-public/AB6AXuCaUIGJYBUgEdoCwWDicpWvcYQtxjzK9Av0tZQlmvpYoLA6IE9kAxW_MuEId8AQQ3d1xEJGKCdNpCkxtK8SIwF-ViV2TazKoCjNB3Br60nRWctFp2idTfuc1rCcV3sjjyaT90ohsEBi8lBAe1XNBSBYAur_wlayiXgGGhbAKbk_2fHHvSzzrf1VoYm2XCaSMoYO8hIkF8y45V9r1cdd0Ulag02loHvN7pBIMEBkz80uFSz0DTEb4Os1xw"/>
<span class="absolute top-3 left-3 bg-surface-container-lowest/80 backdrop-blur-md text-on-surface border border-outline-variant/30 px-2 py-0.5 rounded text-label-sm font-label-sm font-semibold">T1:E2</span>
<div class="absolute inset-0 bg-background/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
<div class="w-12 h-12 rounded-full bg-primary-container text-on-primary flex items-center justify-center shadow-lg">
<span class="material-symbols-outlined text-3xl font-bold" data-icon="play_arrow" style="font-variation-settings: 'FILL' 1;">play_arrow</span>
</div>
</div>
<div class="absolute bottom-0 left-0 w-full h-1.5 bg-surface-container-highest">
<div class="h-full bg-primary-container shadow-sm shadow-primary" style="width: 42%;"></div>
</div>
</div>
<div class="mt-2.5 flex justify-between items-baseline">
<h3 class="text-label-lg font-label-lg font-bold text-on-surface truncate">Planeta Extremo: El Hielo Infinito</h3>
<span class="text-label-sm font-label-sm text-primary flex-shrink-0">31 min rest.</span>
</div>
<p class="text-body-md font-body-md text-on-surface-variant text-sm truncate">Episodio 2: Secretos del Círculo Ártico</p>
</div>
<!-- Card 3 -->
<div class="flex-shrink-0 w-80 group cursor-pointer transition-transform duration-200 hover:scale-105 focus:scale-105">
<div class="relative w-full aspect-video rounded-2xl overflow-hidden bg-surface-container-low border border-outline-variant/30 group-hover:border-primary group-focus:border-primary group-hover:shadow-lg group-hover:shadow-primary/20 transition-all">
<img class="w-full h-full object-cover" data-alt="A dramatic high-speed motorsports action frame capturing aerodynamic racecars hurtling down an asphalt circuit at twilight, with bright brake rotor glows and atmospheric track lighting creating dynamic motion streaks." src="https://lh3.googleusercontent.com/aida-public/AB6AXuCjVCIdEKId_0sp1z8pV3X2t95oi1PvGV0eHr9N-oNM8Groi_AYWG9RYEzYBHckrCd78yM3DDpzIhWP6fw_eEu2koFgcDaiocFGgYYZCiCq559K2MxnXzuzIbvRpXcm_cB_UoHGOUKG01H08_xwfGjcGTP9ncuuPqj6wrlA98Ed31TZ3qz9WHvTq920HKB89gTgGDPqsMtEglRGzKOrG5pZSq1m5xxNvuAUOdpGtr43iCvJGEhcHJ8lig"/>
<span class="absolute top-3 left-3 bg-surface-container-lowest/80 backdrop-blur-md text-on-surface border border-outline-variant/30 px-2 py-0.5 rounded text-label-sm font-label-sm font-semibold">Temporada 5</span>
<div class="absolute inset-0 bg-background/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
<div class="w-12 h-12 rounded-full bg-primary-container text-on-primary flex items-center justify-center shadow-lg">
<span class="material-symbols-outlined text-3xl font-bold" data-icon="play_arrow" style="font-variation-settings: 'FILL' 1;">play_arrow</span>
</div>
</div>
<div class="absolute bottom-0 left-0 w-full h-1.5 bg-surface-container-highest">
<div class="h-full bg-primary-container shadow-sm shadow-primary" style="width: 89%;"></div>
</div>
</div>
<div class="mt-2.5 flex justify-between items-baseline">
<h3 class="text-label-lg font-label-lg font-bold text-on-surface truncate">Vértigo: Carrera por la Gloria</h3>
<span class="text-label-sm font-label-sm text-primary flex-shrink-0">6 min rest.</span>
</div>
<p class="text-body-md font-body-md text-on-surface-variant text-sm truncate">Episodio 8: El Gran Premio de Mónaco</p>
</div>
<!-- Card 4 -->
<div class="flex-shrink-0 w-80 group cursor-pointer transition-transform duration-200 hover:scale-105 focus:scale-105">
<div class="relative w-full aspect-video rounded-2xl overflow-hidden bg-surface-container-low border border-outline-variant/30 group-hover:border-primary group-focus:border-primary group-hover:shadow-lg group-hover:shadow-primary/20 transition-all">
<img class="w-full h-full object-cover" data-alt="A high-fantasy epic scenery of a medieval castle fortress perched atop misty jagged peaks during dusk, with warm candlelit towers set against cold steel-blue alpine skies and flying mythological creatures in the distant clouds." src="https://lh3.googleusercontent.com/aida-public/AB6AXuCu5jJVT18k9NLdSJC05gCcMu8Rid9nE437V7HDi5mw0tFJC0CqFlI5Af8j-vHHTlExSnIh6XiVy_RmEAP3C4-rGmPWNgxGdQKVM2SVvN5ijlKrj9etTeYd0110ZGWzNb_-FLHTCgzLOlOcRyhPLzr9HlcpobnVbNGYkP5F2eaBMnNdi10-_E8bLpuwnVl9M6pemxztotCkhjlZ4hldnZtvRP6YXcgUnFCtci0Vqg5imP8u41tg7YzTjg"/>
<span class="absolute top-3 left-3 bg-surface-container-lowest/80 backdrop-blur-md text-on-surface border border-outline-variant/30 px-2 py-0.5 rounded text-label-sm font-label-sm font-semibold">T3:E1</span>
<div class="absolute inset-0 bg-background/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
<div class="w-12 h-12 rounded-full bg-primary-container text-on-primary flex items-center justify-center shadow-lg">
<span class="material-symbols-outlined text-3xl font-bold" data-icon="play_arrow" style="font-variation-settings: 'FILL' 1;">play_arrow</span>
</div>
</div>
<div class="absolute bottom-0 left-0 w-full h-1.5 bg-surface-container-highest">
<div class="h-full bg-primary-container shadow-sm shadow-primary" style="width: 25%;"></div>
</div>
</div>
<div class="mt-2.5 flex justify-between items-baseline">
<h3 class="text-label-lg font-label-lg font-bold text-on-surface truncate">La Corona de Cenizas</h3>
<span class="text-label-sm font-label-sm text-primary flex-shrink-0">45 min rest.</span>
</div>
<p class="text-body-md font-body-md text-on-surface-variant text-sm truncate">Episodio 1: El Despertar del Dragón</p>
</div>
</div>
</section>
<!-- ================= CARRIL 2: CANALES EN VIVO AHORA (EPG Style) ================= -->
<section class="flex flex-col gap-space-sm">
<div class="flex items-center justify-between">
<div class="flex items-center gap-space-xs">
<span class="material-symbols-outlined text-error text-2xl" data-icon="sensors">sensors</span>
<h2 class="text-headline-sm font-headline-sm text-on-surface font-bold tracking-tight">Canales en Vivo Ahora</h2>
<span class="bg-error/20 text-error border border-error/30 text-label-sm font-label-sm px-2.5 py-0.5 rounded-full uppercase tracking-wider font-bold animate-pulse flex items-center gap-1.5 ml-2">
<span class="w-2 h-2 rounded-full bg-error"></span> EN VIVO
            </span>
</div>
<span class="text-label-md font-label-md text-on-surface-variant hover:text-primary transition-colors cursor-pointer flex items-center gap-1">
            Guía EPG Completa <span class="material-symbols-outlined text-lg" data-icon="arrow_forward_ios">arrow_forward_ios</span>
</span>
</div>
<!-- Live Channels Grid / Row -->
<div class="flex gap-gutter-tv overflow-x-auto custom-scrollbar pt-2 pb-4">
<!-- Live Tile 1: ESPN HD -->
<div class="flex-shrink-0 w-80 bg-surface-container-low rounded-2xl border border-outline-variant/30 p-3 hover:border-primary hover:shadow-xl hover:shadow-primary/20 transition-all duration-200 group cursor-pointer hover:scale-105">
<div class="relative w-full h-36 rounded-xl overflow-hidden bg-surface-container-lowest">
<img class="w-full h-full object-cover" data-alt="A wide panoramic television broadcast shot of an elite European soccer tournament final inside a massive illuminated stadium packed with cheering fans, featuring dramatic floodlights and intense dynamic on-pitch action." src="https://lh3.googleusercontent.com/aida-public/AB6AXuC5__r4Ga4HbdR-eaouwJXEy2mAqJO6w8yItFSER4zK96pwMennTv-pccTHf_nwG6JTsdkOW6ig3ilTUSNeAXVYwdhFnAF-wrDWy0ONwgwGSVOIKzD8FLPb5bxFgC3pUzcmGM5jvE6IMDD_7-S7jisTEryHs5M1qfUQjRMDKtwsxDGryWWA-noNShP3V4mbWWTw7eZft9M0fII02-h2-kN59k7sVXi2yXV9RKQ4zrkiykyRLiZW67K38g"/>
<div class="absolute top-2.5 left-2.5 bg-black/70 backdrop-blur-md px-2 py-0.5 rounded text-white font-headline-sm text-label-sm font-bold tracking-wider">
                ESPN HD
              </div>
<span class="absolute top-2.5 right-2.5 bg-error text-white font-label-sm font-bold px-2 py-0.5 rounded text-xs tracking-wider">
                LIVE 4K
              </span>
<div class="absolute bottom-0 left-0 w-full h-1 bg-surface-container-high">
<div class="h-full bg-primary" style="width: 75%;"></div>
</div>
</div>
<div class="mt-3">
<h3 class="text-label-lg font-label-lg font-bold text-on-surface truncate">UEFA Champions League: Semifinal</h3>
<div class="flex items-center justify-between text-label-sm font-label-sm text-on-surface-variant mt-1">
<span class="text-primary font-semibold">Minuto 68' (En Curso)</span>
<span>Termina: 21:15</span>
</div>
<div class="mt-2 pt-2 border-t border-outline-variant/20 flex items-center gap-1 text-label-sm text-on-surface-variant/70 truncate">
<span class="text-primary font-bold">Sig:</span> SportsCenter Edición Especial
              </div>
</div>
</div>
<!-- Live Tile 2: HBO Premium -->
<div class="flex-shrink-0 w-80 bg-surface-container-low rounded-2xl border border-outline-variant/30 p-3 hover:border-primary hover:shadow-xl hover:shadow-primary/20 transition-all duration-200 group cursor-pointer hover:scale-105">
<div class="relative w-full h-36 rounded-xl overflow-hidden bg-surface-container-lowest">
<img class="w-full h-full object-cover" data-alt="A dramatic high-budget cinematic crime drama broadcast visual showing two rival protagonists in formal evening attire confronting each other in a dimly lit luxurious penthouse with floor-to-ceiling glass windows overlooking a stormy night skyline." src="https://lh3.googleusercontent.com/aida-public/AB6AXuBODOt7fW-mAeEXZv5LypXuGNDR5uvvHKPYHweGnKxY9csnKJC_HxKtkkMHcmsR-EWpBw9ViWU-Km6RjYMi7VVOqQ_eSijJKGVuQv5H7NtzmjutJ0hM2ZOOsMFB1reHY6e3-5q0-PjjQ4ta5eP4cQD0KZRcbTMqs1YYq_zDTp3kLGHmoCbpJYrgcZwkThOUPvNuoky2hg_GUWY-4t1yIPmDh5uZW3hAqZtS28m7TO8c622A9ukVUqEDug"/>
<div class="absolute top-2.5 left-2.5 bg-black/70 backdrop-blur-md px-2 py-0.5 rounded text-white font-headline-sm text-label-sm font-bold tracking-wider">
                HBO PRIME
              </div>
<span class="absolute top-2.5 right-2.5 bg-error text-white font-label-sm font-bold px-2 py-0.5 rounded text-xs tracking-wider">
                LIVE
              </span>
<div class="absolute bottom-0 left-0 w-full h-1 bg-surface-container-high">
<div class="h-full bg-primary" style="width: 35%;"></div>
</div>
</div>
<div class="mt-3">
<h3 class="text-label-lg font-label-lg font-bold text-on-surface truncate">Succession: El Último Baile</h3>
<div class="flex items-center justify-between text-label-sm font-label-sm text-on-surface-variant mt-1">
<span class="text-primary font-semibold">20:15 - 21:30</span>
<span>45 min rest.</span>
</div>
<div class="mt-2 pt-2 border-t border-outline-variant/20 flex items-center gap-1 text-label-sm text-on-surface-variant/70 truncate">
<span class="text-primary font-bold">Sig:</span> The White Lotus (Estreno)
              </div>
</div>
</div>
<!-- Live Tile 3: Discovery Science -->
<div class="flex-shrink-0 w-80 bg-surface-container-low rounded-2xl border border-outline-variant/30 p-3 hover:border-primary hover:shadow-xl hover:shadow-primary/20 transition-all duration-200 group cursor-pointer hover:scale-105">
<div class="relative w-full h-36 rounded-xl overflow-hidden bg-surface-container-lowest">
<img class="w-full h-full object-cover" data-alt="A macro scientific documentary frame showing glowing microscopic marine microorganisms phosphorescing in deep sea waters, revealing intricate crystalline biological structures in luminescent electric cyan and violet hues." src="https://lh3.googleusercontent.com/aida-public/AB6AXuAgiGq1RmSNTwe3Ol2ka5icoVWvqgyLnhcC5SGXstnu9gkt28MT8ckVOVGPCHQ9F7nbWcJsxVT1fqC0Rud8IMTjBcVjB761Sy0V11JSPo7R31xq93Q5-hUQ13G1QvMKvn_ZduG0odJp7sX6kqJ9dII1famMCTvgd346pDQPSBZt69BseTS02VM4egbIJsuocA7XNoHZnUOgZh6yBmW9tqHjaotAvv0moRkMeSLF-LA5Gw1O4FALaW4Plw"/>
<div class="absolute top-2.5 left-2.5 bg-black/70 backdrop-blur-md px-2 py-0.5 rounded text-white font-headline-sm text-label-sm font-bold tracking-wider">
                DISCOVERY
              </div>
<span class="absolute top-2.5 right-2.5 bg-error text-white font-label-sm font-bold px-2 py-0.5 rounded text-xs tracking-wider">
                LIVE 4K
              </span>
<div class="absolute bottom-0 left-0 w-full h-1 bg-surface-container-high">
<div class="h-full bg-primary" style="width: 50%;"></div>
</div>
</div>
<div class="mt-3">
<h3 class="text-label-lg font-label-lg font-bold text-on-surface truncate">Misterios del Abismo Oceánico</h3>
<div class="flex items-center justify-between text-label-sm font-label-sm text-on-surface-variant mt-1">
<span class="text-primary font-semibold">20:30 - 21:30</span>
<span>30 min rest.</span>
</div>
<div class="mt-2 pt-2 border-t border-outline-variant/20 flex items-center gap-1 text-label-sm text-on-surface-variant/70 truncate">
<span class="text-primary font-bold">Sig:</span> Universo Profundo: Agujeros Negros
              </div>
</div>
</div>
<!-- Live Tile 4: Cinemax Hits -->
<div class="flex-shrink-0 w-80 bg-surface-container-low rounded-2xl border border-outline-variant/30 p-3 hover:border-primary hover:shadow-xl hover:shadow-primary/20 transition-all duration-200 group cursor-pointer hover:scale-105">
<div class="relative w-full h-36 rounded-xl overflow-hidden bg-surface-container-lowest">
<img class="w-full h-full object-cover" data-alt="A thrilling cinematic explosive chase sequence from an espionage blockbuster with dark armored SUVs navigating through a blazing downtown tunnel in brilliant theatrical high contrast and volumetric smoke." src="https://lh3.googleusercontent.com/aida-public/AB6AXuBxFcycac4zTfl7D67zSKoE5ihtjtOCv3Q3GtUPwYtXEUyqZI3YoLjqCqcDGaOkT0qtcUCXVxIcie57Kx-8JgvAXvloVj66K559SmchC5tqOKRKjtNwK2m8W-Cmel7_p0iRpC4jXc1gQfhmRGs7mN_zPKZdPNapgXPkeGur2Roexhux9s1mReo74S6iLfYr022KOdq3QNTTa4y90Dqqtg2Y7LnkQ5RKliAmI1tiZPuAnBlPhlPte5u__Q"/>
<div class="absolute top-2.5 left-2.5 bg-black/70 backdrop-blur-md px-2 py-0.5 rounded text-white font-headline-sm text-label-sm font-bold tracking-wider">
                CINEMAX
              </div>
<span class="absolute top-2.5 right-2.5 bg-error text-white font-label-sm font-bold px-2 py-0.5 rounded text-xs tracking-wider">
                LIVE
              </span>
<div class="absolute bottom-0 left-0 w-full h-1 bg-surface-container-high">
<div class="h-full bg-primary" style="width: 82%;"></div>
</div>
</div>
<div class="mt-3">
<h3 class="text-label-lg font-label-lg font-bold text-on-surface truncate">Misión Letal: Punto de Impacto</h3>
<div class="flex items-center justify-between text-label-sm font-label-sm text-on-surface-variant mt-1">
<span class="text-primary font-semibold">20:00 - 22:00</span>
<span>15 min rest.</span>
</div>
<div class="mt-2 pt-2 border-t border-outline-variant/20 flex items-center gap-1 text-label-sm text-on-surface-variant/70 truncate">
<span class="text-primary font-bold">Sig:</span> El Vengador Silencioso
              </div>
</div>
</div>
</div>
</section>
<!-- ================= CARRIL 3: PELÍCULAS MÁS VISTAS (2:3 Vertical Posters) ================= -->
<section class="flex flex-col gap-space-sm mb-12">
<div class="flex items-center justify-between">
<div class="flex items-center gap-space-xs">
<span class="material-symbols-outlined text-secondary text-2xl" data-icon="local_fire_department">local_fire_department</span>
<h2 class="text-headline-sm font-headline-sm text-on-surface font-bold tracking-tight">Películas Más Vistas en IPTV Premium</h2>
</div>
<span class="text-label-md font-label-md text-on-surface-variant hover:text-primary transition-colors cursor-pointer flex items-center gap-1">
            Top 100 <span class="material-symbols-outlined text-lg" data-icon="arrow_forward_ios">arrow_forward_ios</span>
</span>
</div>
<!-- 2:3 Vertical Poster Rail -->
<div class="flex gap-gutter-tv overflow-x-auto custom-scrollbar pt-2 pb-6">
<!-- Poster 1: Top 1 -->
<div class="flex-shrink-0 w-52 group cursor-pointer transition-transform duration-200 hover:scale-105 focus:scale-105">
<div class="relative w-full aspect-[2/3] rounded-2xl overflow-hidden bg-surface-container-low border border-outline-variant/30 group-hover:border-primary group-hover:shadow-2xl group-hover:shadow-primary/30 transition-all">
<img class="w-full h-full object-cover" data-alt="A dramatic vertical movie poster artwork depicting a solitary cybernetic astronaut facing a gigantic luminous dimensional portal on an alien obsidian mountain, with radiant cyan and deep celestial indigo cosmic nebulae." src="https://lh3.googleusercontent.com/aida-public/AB6AXuAAUxDFIf2VBFqabsYxEOfnOSAzZ3oTVqUEwExB17bpvZ8znRryjFMp5hSwGMcEy0Moc35-ph2LfHI5-ljJ199rvH4TENhmciHHR3hn2dU2nfftqKD3y3ljAIdCjM93dfgMZ3o52J41pVGVswhg16l6V0j2yguDAxq9u6x4PxwKKJYtPksJsVkzMUJBbsXWRo-1491ldRQ1PBlhnvPZbqvCvZwwN-gKXKLnFXvDoAcZ_tVWA85RvlAVuQ"/>
<!-- Top Rank Gold Badge -->
<div class="absolute top-3 left-3 bg-gradient-to-r from-amber-400 to-amber-600 text-surface-container-lowest font-headline-sm text-headline-sm font-black w-9 h-9 rounded-lg flex items-center justify-center shadow-lg">
                1
              </div>
<div class="absolute top-3 right-3 bg-surface-container-lowest/80 backdrop-blur-md text-primary text-label-sm font-label-sm font-bold px-2 py-0.5 rounded border border-primary/30">
                4K HDR
              </div>
<!-- Ambient Vignette -->
<div class="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-background via-background/40 to-transparent"></div>
<div class="absolute bottom-3 left-3 right-3 text-center">
<span class="bg-primary/20 text-primary text-xs font-bold px-2 py-0.5 rounded-full border border-primary/30">Ciencia Ficción</span>
</div>
</div>
<h3 class="mt-2.5 text-label-lg font-label-lg font-bold text-on-surface truncate">Vórtice Infinito</h3>
<p class="text-label-sm font-label-sm text-on-surface-variant flex items-center justify-between">
<span>2024</span>
<span class="text-secondary font-semibold">★ 9.4</span>
</p>
</div>
<!-- Poster 2: Top 2 -->
<div class="flex-shrink-0 w-52 group cursor-pointer transition-transform duration-200 hover:scale-105 focus:scale-105">
<div class="relative w-full aspect-[2/3] rounded-2xl overflow-hidden bg-surface-container-low border border-outline-variant/30 group-hover:border-primary group-hover:shadow-2xl group-hover:shadow-primary/30 transition-all">
<img class="w-full h-full object-cover" data-alt="A dark neo-noir vertical film poster showing an enigmatic masked vigilante standing on a towering skyscraper gargoyle gazing over a rain-soaked sprawling metropolis lit by neon signage and atmospheric blue fog." src="https://lh3.googleusercontent.com/aida-public/AB6AXuBhV40Gzk0RjnuhAMBAcNhkUeDw2HqM5vyYEnT9Jc0kbEZ4AI9_BXp0JsGXh88bfNW9QjM_EqccvZKOR01fiIsbOM18HJD8vn-L4hpWIOn-Fat1k9VRuq5w8tiiSmNS3pLWaZ8346K1rbc6Y0f1Y0X4MKT2QC3hrslnrSEq21gHZn-yiJkbNLmw1MIWqeD6oqAqJ0_lvgKztorRQCkR0RII5Qqtxiy03Izzm-zcTjMSWEeBQQ_S0WFPEQ"/>
<div class="absolute top-3 left-3 bg-gradient-to-r from-slate-300 to-slate-500 text-surface-container-lowest font-headline-sm text-headline-sm font-black w-9 h-9 rounded-lg flex items-center justify-center shadow-lg">
                2
              </div>
<div class="absolute top-3 right-3 bg-surface-container-lowest/80 backdrop-blur-md text-primary text-label-sm font-label-sm font-bold px-2 py-0.5 rounded border border-primary/30">
                4K UHD
              </div>
<div class="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-background via-background/40 to-transparent"></div>
<div class="absolute bottom-3 left-3 right-3 text-center">
<span class="bg-primary/20 text-primary text-xs font-bold px-2 py-0.5 rounded-full border border-primary/30">Acción / Thriller</span>
</div>
</div>
<h3 class="mt-2.5 text-label-lg font-label-lg font-bold text-on-surface truncate">El Ojo de Gotham</h3>
<p class="text-label-sm font-label-sm text-on-surface-variant flex items-center justify-between">
<span>2023</span>
<span class="text-secondary font-semibold">★ 9.1</span>
</p>
</div>
<!-- Poster 3: Top 3 -->
<div class="flex-shrink-0 w-52 group cursor-pointer transition-transform duration-200 hover:scale-105 focus:scale-105">
<div class="relative w-full aspect-[2/3] rounded-2xl overflow-hidden bg-surface-container-low border border-outline-variant/30 group-hover:border-primary group-hover:shadow-2xl group-hover:shadow-primary/30 transition-all">
<img class="w-full h-full object-cover" data-alt="A breathtaking theatrical vertical poster showcasing an underwater mythical civilization glowing with aquatic azure crystal energy, featuring ancient aquatic warriors in iridescent armor surrounded by deep sea leviathans." src="https://lh3.googleusercontent.com/aida-public/AB6AXuB_IQJff7k0MKE5fGl0WK2od2nPJvVguHptyoR8YcA04iYAPM5sigiTNdmB6Kq7suyGUUbC9h0X_Or7_qHND3U29K0vnz8U3Oc5yx2L646waWuyoli7A2X-ChLMpr-rJZmvDtZvYVeBX4l5DVfqAdN1yrL2-mm8Qr6d_3R4mNwHk5lQVFwehHvoLagNGfG_x2BMMwmlbMZgZa5LlVAg9jh7HjeNabacE-FIUip4bDiTuPu2sPG5tEASlg"/>
<div class="absolute top-3 left-3 bg-gradient-to-r from-amber-600 to-amber-800 text-surface-container-lowest font-headline-sm text-headline-sm font-black w-9 h-9 rounded-lg flex items-center justify-center shadow-lg">
                3
              </div>
<div class="absolute top-3 right-3 bg-surface-container-lowest/80 backdrop-blur-md text-primary text-label-sm font-label-sm font-bold px-2 py-0.5 rounded border border-primary/30">
                Dolby Vision
              </div>
<div class="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-background via-background/40 to-transparent"></div>
<div class="absolute bottom-3 left-3 right-3 text-center">
<span class="bg-primary/20 text-primary text-xs font-bold px-2 py-0.5 rounded-full border border-primary/30">Fantasía Épica</span>
</div>
</div>
<h3 class="mt-2.5 text-label-lg font-label-lg font-bold text-on-surface truncate">Reino de las Mareas</h3>
<p class="text-label-sm font-label-sm text-on-surface-variant flex items-center justify-between">
<span>2024</span>
<span class="text-secondary font-semibold">★ 8.9</span>
</p>
</div>
<!-- Poster 4: Top 4 -->
<div class="flex-shrink-0 w-52 group cursor-pointer transition-transform duration-200 hover:scale-105 focus:scale-105">
<div class="relative w-full aspect-[2/3] rounded-2xl overflow-hidden bg-surface-container-low border border-outline-variant/30 group-hover:border-primary group-hover:shadow-2xl group-hover:shadow-primary/30 transition-all">
<img class="w-full h-full object-cover" data-alt="A stylish vertical poster of a psychological thriller showcasing the split-face duality of a suave mastermind in deep monochrome chiaroscuro lighting pierced by vibrant razor-sharp cyan laser lines." src="https://lh3.googleusercontent.com/aida-public/AB6AXuBXYAFhRcn-4H5qZ8t6TlMX7xUeS3sUlVVzItHl_b9IoWuWRr1hq5DZj58CkCk9-NqetiVsBhg9d_MyYsXdFh4Ac4Ka-QUjS5sJZs_NMnTskJxY3n_FuWQQPTcTKZ-w4SQKGzmFIZMs8yvnjm46ffNWM9a5zKsNzc4ah1ld5Wp68v9ZWyi8IIBo1VBS1jlc7b5AlF0d3MGo5tSpH1fb9Om29uPyxjrmN2B4vLGG-1MgTUfRWe_dLbDjZQ"/>
<div class="absolute top-3 left-3 bg-surface-container-highest text-on-surface font-headline-sm text-headline-sm font-black w-9 h-9 rounded-lg flex items-center justify-center border border-outline-variant/40 shadow-lg">
                4
              </div>
<div class="absolute top-3 right-3 bg-surface-container-lowest/80 backdrop-blur-md text-primary text-label-sm font-label-sm font-bold px-2 py-0.5 rounded border border-primary/30">
                4K UHD
              </div>
<div class="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-background via-background/40 to-transparent"></div>
<div class="absolute bottom-3 left-3 right-3 text-center">
<span class="bg-primary/20 text-primary text-xs font-bold px-2 py-0.5 rounded-full border border-primary/30">Suspense</span>
</div>
</div>
<h3 class="mt-2.5 text-label-lg font-label-lg font-bold text-on-surface truncate">Mente Cautiva</h3>
<p class="text-label-sm font-label-sm text-on-surface-variant flex items-center justify-between">
<span>2024</span>
<span class="text-secondary font-semibold">★ 8.7</span>
</p>
</div>
<!-- Poster 5: Top 5 -->
<div class="flex-shrink-0 w-52 group cursor-pointer transition-transform duration-200 hover:scale-105 focus:scale-105">
<div class="relative w-full aspect-[2/3] rounded-2xl overflow-hidden bg-surface-container-low border border-outline-variant/30 group-hover:border-primary group-hover:shadow-2xl group-hover:shadow-primary/30 transition-all">
<img class="w-full h-full object-cover" data-alt="An intense post-apocalyptic cinematic poster of an exploratory convoy traversing an endless crystalline salt desert under twin suns with dust clouds catching warm copper and cool electric blue lighting." src="https://lh3.googleusercontent.com/aida-public/AB6AXuAweZaz59JySOzUxEnqsQIO8sDZGUyj44fS_lKfr_23JUFbLgvpkh0ZKIzslNLXw37oO8EhNVho-Ts2n4T7Dhhuzou9PDlQwnV3XnVLTPMFiXIaJg3Ctj0WqyG2YeQYoDPPS1oRiNbDFSlq0ZL416BJMQM1kHlsEBnH84OI4Yah6KfwSs1t3zlbzt4xBmCvps3yAOlEHw6Br5UgD9n949xP_-W8jFBR4CkUJMfZtPs-fvscxWysPuBJwg"/>
<div class="absolute top-3 left-3 bg-surface-container-highest text-on-surface font-headline-sm text-headline-sm font-black w-9 h-9 rounded-lg flex items-center justify-center border border-outline-variant/40 shadow-lg">
                5
              </div>
<div class="absolute top-3 right-3 bg-surface-container-lowest/80 backdrop-blur-md text-primary text-label-sm font-label-sm font-bold px-2 py-0.5 rounded border border-primary/30">
                IMAX 4K
              </div>
<div class="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-background via-background/40 to-transparent"></div>
<div class="absolute bottom-3 left-3 right-3 text-center">
<span class="bg-primary/20 text-primary text-xs font-bold px-2 py-0.5 rounded-full border border-primary/30">Aventura</span>
</div>
</div>
<h3 class="mt-2.5 text-label-lg font-label-lg font-bold text-on-surface truncate">Duna Negra: El Origen</h3>
<p class="text-label-sm font-label-sm text-on-surface-variant flex items-center justify-between">
<span>2023</span>
<span class="text-secondary font-semibold">★ 8.8</span>
</p>
</div>
<!-- Poster 6: Top 6 -->
<div class="flex-shrink-0 w-52 group cursor-pointer transition-transform duration-200 hover:scale-105 focus:scale-105">
<div class="relative w-full aspect-[2/3] rounded-2xl overflow-hidden bg-surface-container-low border border-outline-variant/30 group-hover:border-primary group-hover:shadow-2xl group-hover:shadow-primary/30 transition-all">
<img class="w-full h-full object-cover" data-alt="An animated vertical adventure film poster displaying a vibrant bioluminescent enchanted forest filled with miniature glowing woodland creatures and starry midnight foliage in stunning high-gamut 4K HDR color." src="https://lh3.googleusercontent.com/aida-public/AB6AXuAt341PYx2S8RDR2uY7mcJDGKuhwuPsRa-4gKPR6Gb1Va93SXboIweevWnPR0ACQ2WtAFA5F8a0XhPgCKU9N2WM2oclhgakrnox0stIvbEgTeqdHI571m3G5u3EHKzhKg5ziK0A-l_e8kJR8lPFYXbOBx7e5BOk9MwWUmsg6n2qfzOCiP19wP23i9kLIiG6BlmVFklE5ZFFsg33wfbxQ4CqldmKWrlp3ff6JpVrysDUtcQspGkWoi0lBA"/>
<div class="absolute top-3 left-3 bg-surface-container-highest text-on-surface font-headline-sm text-headline-sm font-black w-9 h-9 rounded-lg flex items-center justify-center border border-outline-variant/40 shadow-lg">
                6
              </div>
<div class="absolute top-3 right-3 bg-surface-container-lowest/80 backdrop-blur-md text-primary text-label-sm font-label-sm font-bold px-2 py-0.5 rounded border border-primary/30">
                HDR10+
              </div>
<div class="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-background via-background/40 to-transparent"></div>
<div class="absolute bottom-3 left-3 right-3 text-center">
<span class="bg-primary/20 text-primary text-xs font-bold px-2 py-0.5 rounded-full border border-primary/30">Animación</span>
</div>
</div>
<h3 class="mt-2.5 text-label-lg font-label-lg font-bold text-on-surface truncate">El Guardián del Bosque</h3>
<p class="text-label-sm font-label-sm text-on-surface-variant flex items-center justify-between">
<span>2024</span>
<span class="text-secondary font-semibold">★ 9.0</span>
</p>
</div>
</div>
</section>
</div>
</main>
<!-- ================= 10-FOOT UI REMOTE CONTROL HELPER FOOTER ================= -->
<footer class="fixed bottom-0 left-0 w-full z-40 bg-gradient-to-t from-surface-container-lowest via-surface-container-lowest/90 to-transparent py-3 px-margin-safe pointer-events-none flex items-center justify-end">
<div class="flex items-center gap-space-lg text-on-surface-variant text-label-sm font-label-sm bg-surface-container-high/60 backdrop-blur-md px- space-md py-1.5 rounded-full border border-outline-variant/30">
<!-- Button OK -->
<div class="flex items-center gap-1.5">
<span class="w-5 h-5 rounded-full bg-primary/20 text-primary border border-primary flex items-center justify-center font-bold text-[10px]">OK</span>
<span>Seleccionar</span>
</div>
<!-- Button Volver -->
<div class="flex items-center gap-1.5">
<span class="w-5 h-5 rounded-full bg-surface-container-highest border border-outline flex items-center justify-center font-bold text-[10px] text-on-surface">↩</span>
<span>Volver</span>
</div>
<!-- Button Azul (Opciones) -->
<div class="flex items-center gap-1.5">
<span class="w-3.5 h-3.5 rounded-full bg-primary inline-block"></span>
<span>Opciones IPTV</span>
</div>
<!-- Button Info -->
<div class="flex items-center gap-1.5">
<span class="w-5 h-5 rounded-full bg-surface-container-highest border border-outline flex items-center justify-center font-bold text-[10px] text-on-surface">i</span>
<span>Guía EPG</span>
</div>
</div>
</footer>
</body></html>