package org.fossify.messages.helpers

object BankPeWhitelist {
    private val knownBankPeNames: Set<String> = setOf(
        "HDFCBK", "HDFC", "HDFCB",
        "SBIINB", "SBIBNK", "SBI", "SBIUPI", "SBIBK", "SBIINS",
        "ICICIB", "ICICI", "ICICIT",
        "AXISBK", "AXIS", "AXISB",
        "KOTAKB", "KOTAK", "KOTAKM",
        "YESBNK", "YESB",
        "PNBSMS", "PNB", "PNBBNK",
        "BOIIND", "BOI",
        "CANBNK", "CANARA",
        "UNIONB", "UBI", "UNIONBK",
        "IDFCFB", "IDFC", "IDFCFC",
        "RBLBNK", "RBL",
        "INDUSB", "INDUS",
        "IDBIBK", "IDBI",
        "FEDBNK", "FEDERAL",
        "KARNBK", "KARNATAKA",
        "SOUTHB", "SIB",
        "DBSIN", "DBS",
        "CITIBK", "CITI",
        "HSBC", "HSBCIN",
        "STANC", "SCBANK",
        "BARCIN", "BARC",
        "IPPBNK", "IPB", "INDPST",
        "AUSFB", "AU",
        "EQTBNK", "EQUITAS",
        "BANDHN", "BANDHAN",
        "JKBANK", "JK",
        "SARASB", "SARASW",
        "PAYTMB", "PAYTM",
        "AMZNPB", "AMAZONP",
        "FREECH", "FREECHARG",
        "MOBKWK", "MOBIKWIK",
        "PHONPE", "PHONEPE",
    )

    private val knownGovHeaders: Set<String> = setOf(
        "MYGOV", "UIDAI", "ECI", "MOHFW", "CBSE", "EPFOHO", "EPFO",
        "INCMTX", "INCTAX", "INCOMET",
        "NDMA", "CRPF", "MEA", "RBI",
        "DGHIND", "CGHS", "NHA", "ABHAGV",
        "AADHAR", "AYUSHM", "DIGILK", "DIGILOCKER",
        "GSTHLP", "GSTSMS",
        "RAILTL", "IRCTCS", "IRCTC", "RAILMD",
        "DOTGOI", "DOT", "SANCHR",
        "MUMBPL", "DLPOLI", "DLTRAF", "CYBRCM",
        "EVERSE", "TRACES", "INPOST", "INDPOST",
        "BESCOM", "TPDDL", "MSEDCL", "MAHADC", "BSESDL",
    )

    fun isKnownBankPe(peName: String?): Boolean =
        peName != null && peName.uppercase() in knownBankPeNames

    fun isKnownGovernmentHeader(peName: String?): Boolean =
        peName != null && peName.uppercase() in knownGovHeaders
}
