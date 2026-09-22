package org.fossify.messages.helpers

import android.content.Context
import java.util.regex.Pattern

object FraudFilter {

    enum class Category {
        INBOX,
        PROMOTIONS,
        SPAM,
    }

    data class Verdict(
        val category: Category,
        val score: Int,
        val reasons: List<String>,
    )

    private const val SPAM_THRESHOLD = 90
    private const val PROMO_THRESHOLD = 40
    private const val CONTACT_SCORE_CAP = 20

    private val P_OTP_CONTEXT = Pattern.compile(
        "(?is)(\\b(OTP|One[\\s-]?Time[\\s-]?Password|verification[\\s-]?code|ओटीपी)\\b.{0,60}\\b\\d{4,8}\\b|" +
            "\\b\\d{4,8}\\b.{0,60}\\b(OTP|One[\\s-]?Time[\\s-]?Password|verification[\\s-]?code|ओटीपी)\\b)"
    )
    private val P_DO_NOT_SHARE = Pattern.compile(
        "(?i)\\bdo\\s+not\\s+share\\b.*?\\b(OTP|PIN|CVV|password|पासवर्ड)\\b"
    )
    private val P_TXN_ALERT = Pattern.compile(
        "(?i)\\b(credited|debited|withdrawn|deposited|spent)\\b.*?\\b(A/?C|Acct|account|खाता)\\b.*?\\b(Rs\\.?|INR|₹|रुपये)"
    )
    private val P_AADHAAR_OTP = Pattern.compile(
        "(?i)\\b(Aadhaar|UIDAI|आधार)\\b.*?\\b(OTP|verify|verification)\\b"
    )
    private val P_DELIVERY_TRACKING = Pattern.compile(
        "(?i)\\b(tracking|consignment|AWB|order)\\s*(id|no\\.?|number)?\\s*[:#]?\\s*[A-Z0-9]{6,}"
    )
    private val P_IRCTC_TRAIN = Pattern.compile(
        "(?i)\\b(IRCTC|PNR|train\\s*no\\.?|train\\s*number|seat\\s*no\\.?)\\b"
    )

    private val P_MARKETING_WORDS = Pattern.compile(
        "(?i)\\b(MEGA\\s+SALE|MAHA\\s+SALE|BLOCKBUSTER\\s+SALE|FLASH\\s+SALE|" +
            "BIGGEST\\s+(SALE|DEAL|OFFER|DISCOUNT)|BUMPER\\s+(OFFER|SALE|DEAL)|" +
            "LOOT|GRAB\\s+NOW|" +
            "FLAT\\s+\\d+%|UPTO\\s+\\d+%|UP\\s?TO\\s+\\d+%|" +
            "NEW\\s+LAUNCH|JUST\\s+LANDED|" +
            "WIN\\s+(PRIZES?|CASH|GIFT)|WON\\s+(PRIZES?|CASH|GIFT)|" +
            "FREE\\s+(GIFT|COUPON|VOUCHER)|GIVEAWAY|" +
            "CASHBACK\\s+OF|EXCLUSIVE\\s+(OFFER|DEAL))\\b"
    )
    private val P_RS_AMOUNT_OFFER = Pattern.compile(
        "(?i)(?:Rs\\.?|₹|INR)\\s?\\d{1,2}(?:[,]?\\d{3})*\\b.*?\\b(off|cashback|bonus|reward|coupon)\\b"
    )
    private val P_TANDC = Pattern.compile("(?i)\\b(T&C|T\\s?and\\s?C|terms\\s+apply)\\b")
    private val P_UNSUBSCRIBE = Pattern.compile("(?i)\\bunsubscribe\\b|\\bSTOP\\s+to\\s+\\d{3,5}\\b")
    private val P_CLICK_HERE = Pattern.compile(
        "(?i)\\b(click|tap|visit)\\s+(here|now|link|below|bel\\w*)"
    )

    private val P_SHORTENER = Pattern.compile(
        "(?i)\\b(bit\\.ly|tinyurl\\.com|t\\.ly|cutt\\.ly|rebrand\\.ly|is\\.gd|qrco\\.de|" +
            "wa\\.me|short\\.gy|surl\\.li|tiny\\.cc|ow\\.ly|buff\\.ly|t\\.co|rb\\.gy|" +
            "shorturl\\.at|lnkd\\.in|goo\\.gl)/"
    )
    private val P_SUSPICIOUS_TLD = Pattern.compile(
        "(?i)https?://[^\\s]+\\.(top|xyz|buzz|sbs|click|gq|tk|ml|cf|loan|win|date|review|country|stream|download|party|cricket)\\b"
    )
    private val P_ANY_URL = Pattern.compile(
        "(?i)\\b(?:https?://|www\\.)[^\\s]+\\b"
    )
    private val P_APK_EXT = Pattern.compile(
        "(?i)\\.(apk|xapk|apks)\\b|\\bapk\\b"
    )
    private val P_URL_ONLY_BODY = Pattern.compile(
        "^\\s*(?:https?://|www\\.)\\S+\\s*$"
    )

    private val P_BANK_LANGUAGE = Pattern.compile(
        "(?i)\\b(bank|UPI|KYC|aadhaar|आधार|ஆதார்|ఆధార్|ಆಧಾರ್|account|खाता|கணக்கு|ఖాతా|" +
            "panel|PAN\\s*card|कवाईसी|पैन|பான்|" +
            "card[\\s-]?block|account[\\s-]?block|suspend|expire|expiry|" +
            "ATM|netbank|net[\\s-]?banking|credit\\s*card|debit\\s*card|" +
            "बैंक|வங்கி|బ్యాంక్|ಬ್ಯಾಂಕ್)\\b"
    )
    private val P_URGENCY = Pattern.compile(
        "(?i)\\b(immediately|urgent|tonight|today|now|24\\s*hours|will\\s+be\\s+blocked|" +
            "disconnect|disconnected|expire|expiring|expir(e|y|ed)|last\\s+chance|" +
            "आज|तुरंत|अभी|बंद\\s+हो|" +
            "இன்று|உடனே|இப்போது|" +
            "ఈరోజు|వెంటనే|ఇప్పుడు|" +
            "ಇಂದು|ಈಗ|" +
            "آج|فوراً|اب)\\b"
    )
    private val P_REFUND_KEYWORDS = Pattern.compile(
        "(?i)\\b(refund|mistakenly|wrong[\\s-]?transfer|by\\s+mistake|excess[\\s-]?payment)\\b" +
            ".*?\\b(UPI[\\s-]?PIN|VPA|@[a-z]+|please\\s+return)\\b"
    )
    private val P_PARCEL_SCAM = Pattern.compile(
        "(?i)\\b(parcel|courier|consignment|customs|duty|delivery[\\s-]?fail|undelivered|pending\\s+delivery)\\b"
    )
    private val P_ELECTRICITY_SCAM = Pattern.compile(
        "(?i)\\b(electricity|bijli|बिजली|மின்சாரம்|విద్యుత్|ವಿದ್ಯುತ್|" +
            "disconnect|cut[\\s-]?off|power\\s+cut)\\b" +
            ".*?\\b(tonight|today|9\\.?[0-9]{2}|update|verify|இன்று|ఈరోజు|ಇಂದು)\\b"
    )
    private val P_TASK_JOB_SCAM = Pattern.compile(
        "(?i)\\b(earn|daily|part[\\s-]?time|work[\\s-]?from[\\s-]?home|home[\\s-]?based)\\b" +
            ".{0,40}?\\b(?:₹|Rs\\.?|INR)\\s?\\d{3,5}\\b"
    )
    private val P_OTP_SHARE_FRAUD = Pattern.compile(
        "(?i)\\b(share|provide|tell|send|give|disclose|reveal|forward)\\b.{0,20}\\bOTP\\b.{0,40}?\\b(to|with|at|call|whatsapp|wa)\\b"
    )
    private val P_OTP_SHARE_FRAUD_RECIPIENT = Pattern.compile(
        "(?i)\\b(share|provide|tell|send|give|disclose|reveal|forward)\\b.{0,20}\\bOTP\\b.{0,80}?(https?://|www\\.|\\+?\\d{10,15}|[a-z0-9._-]+@[a-z]+)"
    )
    private val P_OTP_SHARE_NEGATION = Pattern.compile(
        "(?i)\\b(do\\s*not|don'?t|never|do\\s*n't|please\\s*don'?t|no\\s+need\\s+to)\\b.{0,30}\\b(share|provide|tell|send|give|disclose|reveal)\\b.{0,40}\\bOTP\\b"
    )
    private val P_CBI_ARREST = Pattern.compile(
        "(?i)\\b(CBI|police|arrest|narcotics|FIR|warrant|FedEx)\\b" +
            ".{0,30}?\\b(case|FIR|warrant|registered|investigation)\\b"
    )
    private val P_UPI_AUTOPAY_SCAM = Pattern.compile(
        "(?i)\\b(autopay|auto[- ]?debit|mandate|recurring)\\b.{0,40}?\\b(approve|authorise|authorize|UPI[- ]?ID|VPA|click|confirm)\\b"
    )
    private val P_LOAN_APPROVED = Pattern.compile(
        "(?i)\\b(loan|personal[- ]?loan|instant[- ]?loan|pre[- ]?approved)\\b.{0,40}?\\b(approved|sanction|disbursed|claim|process)\\b"
    )
    private val P_CRYPTO_FOREX = Pattern.compile(
        "(?i)\\b(crypto|bitcoin|BTC|ETH|forex|MT4|MT5|trading|profit|guaranteed[- ]?return|2x|3x|10x|10%[- ]?daily)\\b" +
            ".{0,40}?\\b(invest|deposit|signup|sign[- ]?up|join|click|earn)\\b"
    )
    private val P_WHATSAPP_HI_SCAM = Pattern.compile(
        "(?i)\\b(send|reply|message|whatsapp|wa)\\b\\s+(\"?hi\"?|hello|join)\\b.{0,30}?\\b(\\+?\\d{10,15})\\b"
    )
    private val P_FAKE_SHOPPING = Pattern.compile(
        "(?i)\\b(amazn|flipkrt|amaz0n|flipkar+t|am4zon|fl1pkart|myntr4|myntr@|shopcl[u0]es)\\b"
    )
    private val P_NEFT_KYC_SCAM = Pattern.compile(
        "(?i)\\b(NEFT|RTGS|IMPS|UPI|netbanking)\\b.{0,30}?\\b(update|expire|reactivate|verify|suspend|block|frozen)\\b"
    )
    private val P_FAKE_GOVT_SUBSIDY = Pattern.compile(
        "(?i)\\b(PM[-\\s]?Kisan|PMAY|Ujjwala|subsidy|scholarship|free[-\\s]?(LPG|gas|laptop|recharge))\\b.{0,40}?\\b(register|apply|click|claim|fill)\\b"
    )
    private val P_OLA_OLX_FRAUD = Pattern.compile(
        "(?i)\\b(OLX|Quikr|FB[-\\s]?marketplace|carousel)\\b.{0,40}?\\b(QR|scan|UPI|PIN|advance|token)\\b"
    )
    private val P_FAKE_ELECTION_SCAM = Pattern.compile(
        "(?i)\\b(voter[-\\s]?(id|list)|EPIC|election[-\\s]?card|booth)\\b.{0,40}?\\b(update|delete|removed|verify|fine|penalty)\\b"
    )

    fun classify(
        context: Context,
        senderAddress: String,
        body: String,
        hasMmsAttachment: Boolean = false,
    ): Verdict {
        val reasons = mutableListOf<String>()
        var score = 0
        val header = DltHeader.parse(senderAddress)

        val hasFraudMarkers =
            (P_OTP_SHARE_FRAUD_RECIPIENT.matcher(body).find() && !P_OTP_SHARE_NEGATION.matcher(body).find()) ||
            P_SHORTENER.matcher(body).find() ||
            P_SUSPICIOUS_TLD.matcher(body).find() ||
            P_APK_EXT.matcher(body).find() ||
            P_REFUND_KEYWORDS.matcher(body).find() ||
            P_CBI_ARREST.matcher(body).find() ||
            P_UPI_AUTOPAY_SCAM.matcher(body).find() ||
            P_FAKE_SHOPPING.matcher(body).find() ||
            P_ELECTRICITY_SCAM.matcher(body).find() ||
            P_TASK_JOB_SCAM.matcher(body).find() ||
            P_NEFT_KYC_SCAM.matcher(body).find() ||
            P_CRYPTO_FOREX.matcher(body).find() ||
            P_WHATSAPP_HI_SCAM.matcher(body).find() ||
            P_OLA_OLX_FRAUD.matcher(body).find() ||
            P_FAKE_ELECTION_SCAM.matcher(body).find() ||
            (P_PARCEL_SCAM.matcher(body).find() && P_ANY_URL.matcher(body).find())

        if (!hasFraudMarkers) {
            if (P_OTP_CONTEXT.matcher(body).find() ||
                P_DO_NOT_SHARE.matcher(body).find() ||
                P_AADHAAR_OTP.matcher(body).find()
            ) {
                return Verdict(Category.INBOX, -100, listOf("OTP / verification content"))
            }
            if (P_TXN_ALERT.matcher(body).find()) {
                return Verdict(Category.INBOX, -80, listOf("Transactional alert (credit/debit)"))
            }
            if (header.isTransactional && BankPeWhitelist.isKnownBankPe(header.peName)) {
                return Verdict(Category.INBOX, -90, listOf("Bank PE + -T suffix"))
            }
            if (header.isGovernment && BankPeWhitelist.isKnownGovernmentHeader(header.peName)) {
                return Verdict(Category.INBOX, -90, listOf("Government sender"))
            }
            if (P_IRCTC_TRAIN.matcher(body).find()) {
                return Verdict(Category.INBOX, -60, listOf("Train / IRCTC info"))
            }
            if (P_DELIVERY_TRACKING.matcher(body).find() &&
                !P_PARCEL_SCAM.matcher(body).find()
            ) {
                return Verdict(Category.INBOX, -40, listOf("Delivery tracking"))
            }
        }

        when (header.category) {
            DltHeader.Category.PROMOTIONAL -> {
                score += 80
                reasons.add("DLT suffix -P (Promotional)")
            }
            DltHeader.Category.SERVICE -> {
                score += 25
                reasons.add("DLT suffix -S (Service)")
            }
            else -> Unit
        }
        if (DltHeader.isRawMobile(senderAddress) && P_BANK_LANGUAGE.matcher(body).find()) {
            score += 60
            reasons.add("Raw mobile sender impersonating bank/UPI")
        }
        if (header.isCommercialPromotional && P_BANK_LANGUAGE.matcher(body).find()) {
            score += 50
            reasons.add("Promotional header but banking/KYC language (suffix mismatch)")
        }

        if (P_SHORTENER.matcher(body).find()) {
            score += 25
            reasons.add("Contains URL shortener")
        }
        if (P_SUSPICIOUS_TLD.matcher(body).find()) {
            score += 50
            reasons.add("Suspicious TLD (.top/.xyz/.click/etc.)")
        }
        if (P_APK_EXT.matcher(body).find()) {
            score += 90
            reasons.add("Mentions APK file (malware-drop pattern)")
        }
        if (P_URL_ONLY_BODY.matcher(body.trim()).matches()) {
            score += 35
            reasons.add("Body is a bare URL")
        }

        if (P_REFUND_KEYWORDS.matcher(body).find()) {
            score += 70
            reasons.add("UPI 'refund / wrong transfer' scam pattern")
        }
        if (P_PARCEL_SCAM.matcher(body).find() && P_ANY_URL.matcher(body).find()) {
            score += 60
            reasons.add("Parcel / courier scam + URL")
        }
        if (P_ELECTRICITY_SCAM.matcher(body).find()) {
            score += 70
            reasons.add("Electricity disconnect scam pattern")
        }
        if (P_TASK_JOB_SCAM.matcher(body).find()) {
            score += 60
            reasons.add("Earn-daily / task scam pattern")
        }
        if (P_OTP_SHARE_FRAUD_RECIPIENT.matcher(body).find() && !P_OTP_SHARE_NEGATION.matcher(body).find()) {
            score += 80
            reasons.add("'Share OTP with X' fraud pattern (recipient present)")
        }
        if (P_CBI_ARREST.matcher(body).find()) {
            score += 80
            reasons.add("Digital-arrest / CBI threat pattern")
        }
        if (P_UPI_AUTOPAY_SCAM.matcher(body).find()) {
            score += 70
            reasons.add("UPI autopay/mandate approval scam")
        }
        if (P_LOAN_APPROVED.matcher(body).find() && P_ANY_URL.matcher(body).find()) {
            score += 50
            reasons.add("Instant-loan approved + URL (predatory lending)")
        }
        if (P_CRYPTO_FOREX.matcher(body).find()) {
            score += 75
            reasons.add("Crypto/forex investment fraud pattern")
        }
        if (P_WHATSAPP_HI_SCAM.matcher(body).find()) {
            score += 65
            reasons.add("'Send Hi to WhatsApp number' redirect scam")
        }
        if (P_FAKE_SHOPPING.matcher(body).find()) {
            score += 85
            reasons.add("Misspelled brand impersonation (Amazn/Flipkrt/Myntr4 etc.)")
        }
        if (P_NEFT_KYC_SCAM.matcher(body).find() && !P_TXN_ALERT.matcher(body).find()) {
            score += 70
            reasons.add("NEFT/UPI 'verify-or-suspend' scam pattern")
        }
        if (P_FAKE_GOVT_SUBSIDY.matcher(body).find() && !header.isGovernment) {
            score += 70
            reasons.add("Fake government subsidy/PMAY/Ujjwala scam")
        }
        if (P_OLA_OLX_FRAUD.matcher(body).find()) {
            score += 60
            reasons.add("OLX/marketplace UPI-QR-scan advance-payment scam")
        }
        if (P_FAKE_ELECTION_SCAM.matcher(body).find() && !header.isGovernment) {
            score += 60
            reasons.add("Fake voter-id/election-card scam")
        }

        var marketingHits = 0
        val marketingMatcher = P_MARKETING_WORDS.matcher(body)
        while (marketingMatcher.find()) marketingHits++
        if (marketingHits > 0) {
            score += (marketingHits * 12).coerceAtMost(40)
            reasons.add("Marketing words x$marketingHits")
        }
        if (P_RS_AMOUNT_OFFER.matcher(body).find()) {
            score += 30
            reasons.add("Money-amount + offer language")
        }
        if (P_TANDC.matcher(body).find()) {
            score += 20
            reasons.add("'T&C apply' advertiser tell")
        }
        if (P_UNSUBSCRIBE.matcher(body).find()) {
            score += 25
            reasons.add("Has 'unsubscribe' / STOP-to-shortcode tell")
        }
        if (P_CLICK_HERE.matcher(body).find()) {
            score += 15
            reasons.add("'Click here / tap below' language")
        }

        val urgencyMatcher = P_URGENCY.matcher(body)
        if (urgencyMatcher.find() && P_ANY_URL.matcher(body).find() && P_SUSPICIOUS_TLD.matcher(body).find()) {
            score += 20
            reasons.add("Urgency language + URL on suspicious TLD (scam-funnel pattern)")
        }

        val words = body.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val asciiLetterWords = words.filter { wordTok ->
            wordTok.length > 2 && wordTok.any { ch -> ch in 'A'..'Z' || ch in 'a'..'z' }
        }
        if (asciiLetterWords.size >= 6) {
            val capsCount = asciiLetterWords.count { wordTok ->
                val asciiChars = wordTok.filter { ch -> ch in 'A'..'Z' || ch in 'a'..'z' }
                asciiChars.length >= 2 && asciiChars == asciiChars.uppercase()
            }
            val capsRatio = capsCount.toDouble() / asciiLetterWords.size
            if (capsRatio > 0.4) {
                score += 20
                reasons.add("All-caps density (${(capsRatio * 100).toInt()}%)")
            }
        }

        if (hasMmsAttachment) {
            val textLen = body.trim().length
            val vowelRatio = if (textLen > 0) {
                body.count { it in "aeiouAEIOUअआइईउऊएऐओऔ" }.toDouble() / textLen
            } else 0.0
            if ((textLen < 40 || vowelRatio < 0.15) && textLen >= 8) {
                score += 70
                reasons.add("MMS: long illegible heading + attachment-only pattern")
            }
        }

        if (isContactSync(context, senderAddress)) {
            if (score > CONTACT_SCORE_CAP) {
                reasons.add("Contact override: capping score (was $score)")
                score = CONTACT_SCORE_CAP
            }
        }

        val category = when {
            score >= SPAM_THRESHOLD -> Category.SPAM
            score >= PROMO_THRESHOLD -> Category.PROMOTIONS
            else -> Category.INBOX
        }
        return Verdict(category, score, reasons)
    }

    private fun isContactSync(context: Context, address: String): Boolean = runCatching {
        val uri = android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI
            .buildUpon().appendPath(address).build()
        context.contentResolver.query(
            uri,
            arrayOf(android.provider.ContactsContract.PhoneLookup._ID),
            null, null, null
        )?.use { c -> c.count > 0 } ?: false
    }.getOrDefault(false)
}
