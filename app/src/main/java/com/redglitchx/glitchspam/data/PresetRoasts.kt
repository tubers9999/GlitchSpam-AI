package com.redglitchx.glitchspam.data

import kotlin.random.Random

object PresetRoasts {

    val ROMANIAN_ROASTS = listOf(
        "Taci măi inteligență la mâna a doua, că dacă tăceai filozof rămâneai, acum doar confirmi diagnoza.",
        "Ai un creier atât de neted încât gândurile tale fac drifting pe el.",
        "Dacă prostia ar durea, ai urla non-stop la ecran.",
        "Vorbești de parcă ai pus creierul pe modul avion și ai uitat parola de la Wi-Fi.",
        "Ești genul de om care se uită pe ambele părți când trece o stradă cu sens unic și tot e călcat de bicicletă.",
        "Mai bine tăceai și lăsai lumea să creadă că ești prost, decât să scrii mesajul ăsta și să elimini orice dubiu.",
        "Până și tastatura ta plânge când vede ce aberații scrii cu două degete.",
        "Nivelul tău de IQ e direct proporțional cu temperatura din congelator.",
        "Dacă aș vrea să mă sinucid, m-aș arunca de la nivelul ego-ului tău la nivelul IQ-ului tău.",
        "Scrii mesaje de parcă ai dat cu capul de tastatură și ai apăsat send din greșeală.",
        "Nu mă cobor la mintea ta, că nu am adus scara de incendiu.",
        "Ești motivul pentru care pe sticlele de șampon scrie 'Nu beți'.",
        "Singurul lucru mai lent decât conexiunea ta la net este viteza cu care procesezi realitatea.",
        "Ești ca un update de Windows la 99%: enervant, inutil și toată lumea vrea să te anuleze.",
        "Când s-a împărțit mintea în univers, tu erai la coadă la semințe.",
        "Ar trebui să ceri scuze copacilor pentru tot oxigenul pe care îl irosești respirând.",
        "Te bați cu logica și pierzi prin knockout în prima secundă.",
        "Nu știu cine te-a mințit că ai o opinie valoroasă, dar merită amendă pentru dezinformare.",
        "Ai vocabularul unui NPC buguit dintr-un joc rusesc din 2003.",
        "Dacă tăceai, păreai măcar misterios. Așa doar confirmi că ești pe avarii.",
        "Nici măcar autocorrect-ul nu mai încearcă să te salveze, a renunțat și el.",
        "Ești dovada vie că evoluția mai ia și pauze de masă.",
        "Vezi că ți-a scăpat logica pe jos, dar nu te apleca să o iei că o calci în picioare.",
        "Dacă prostia ar fi un sport olimpic, ai lua medalia de aur chiar dacă ai concura singur.",
        "Îți dau 2 lei să taci 5 minute, deși nici atât nu meriți pentru calitatea emisiunii tale.",
        "Nu te mai chinui să gândești, că miroase a ars până aici prin ecran.",
        "Ești ca o reclamă neskipabilă de 30 de secunde pe YouTube: absolut nimeni nu te vrea.",
        "Tragi concluzii la fel de repede cum pierzi conexiunea cu bunul simț.",
        "Ai pretenții de campion, dar joci în liga a cincea la fotbal pe maidan.",
        "Du-te și fă o pauză de la tastatură, că ai obosit serverele degeaba.",
        "Când deschizi gura, scade media de inteligență a întregului grup cu 40%.",
        "Ai atâta dreptate câtă apă are Sahara în mijlocul verii.",
        "Dacă aveai două creiere, tot nu erai capabil să legi o idee coerentă.",
        "Stai liniștit, nimeni nu se aștepta la nimic inteligent de la tine oricum.",
        "Ești definiția erorii 404: Creierul nu a fost găsit pe acest server.",
        "Scrii mult și prost, exact ca manualele expirate de instrucțiuni.",
        "Mai lasă tastatura și mai ia o gură de aer curat, poate ți se oxigenează circuitele.",
        "Până și botul ăsta are mai mult suflet și logică decât toate generațiile tale adunate.",
        "Îți recomand să dai restart la personalitate, că ai prins un virus grav de mediocritate.",
        "Hai las-o jos că măcăne, nu impresionezi pe nimeni cu teatrul ăsta ieftin."
    )

    val ENGLISH_ROASTS = listOf(
        "I'd agree with you but then we'd both be wrong.",
        "You bring everyone so much joy... whenever you leave the chat.",
        "Your brain has two gears: off and reverse.",
        "I'm not saying you're dumb, but you make autocorrect look like a genius.",
        "I’ve seen better comebacks from a broken toaster.",
        "You're like a software update when I'm in a rush: nobody asked for you.",
        "Somewhere out there, a tree is working hard to produce oxygen for you. Go apologize to it.",
        "If stupidity was a currency, you'd be a billionaire.",
        "You talk like your keyboard is missing half its letters.",
        "I'd roast you, but my mom told me not to burn trash."
    )

    fun getRandomRoast(language: String): String {
        return when (language.lowercase()) {
            "română", "romanian", "ro" -> ROMANIAN_ROASTS[Random.nextInt(ROMANIAN_ROASTS.size)]
            else -> ENGLISH_ROASTS[Random.nextInt(ENGLISH_ROASTS.size)]
        }
    }
}
