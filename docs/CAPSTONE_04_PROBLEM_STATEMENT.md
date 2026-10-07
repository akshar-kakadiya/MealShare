# MealShare Problem Statement

> **Course**: UQ25CA741A_Capstone Project Phase-1  
> **Milestone**: Presentation-2  
> **Document Version**: 1.0  
> **Date**: September 26, 2026

---

## 1. Problem Context: The Surplus Food Paradox

According to the United Nations Environment Programme (UNEP) Food Waste Index Report (2024), approximately one billion meals are wasted globally every single day, with the food service, retail, and household sectors contributing disproportionately to urban organic waste. Simultaneously, hundreds of millions of people experience acute nutritional deficits. 

In metropolitan urban centers, commercial food establishments—including hotels, restaurants, wedding and event banquet halls, corporate cafeterias, and institutional dining facilities—frequently generate substantial volumes of wholesome, freshly prepared surplus food. However, the vast majority of this surplus food is discarded into municipal landfills rather than redirected to charitable agencies, creating an acute socio-economic and environmental crisis.

---

## 2. Core Operational Challenges

An in-depth analysis of community food rescue operations reveals that surplus food disposal is driven not by an absence of willing donors or hungry populations, but by **critical systemic and logistical failure points**:

### 2.1 Acute Temporal Perishability
Unlike non-perishable consumer goods, prepared meals and cooked food possess an extremely tight consumption window—typically between **3 to 6 hours** from preparation. Any friction or communication latency between the point of surplus generation and recipient discovery results in the food becoming microbiologically unsafe, compelling donors to dispose of meals rather than risk distribution.

### 2.2 Severe Information Asymmetry & Lack of Proximity Visibility
Food donors operate in geographical silos. A banquet hall with 100 surplus meals at 10:00 PM has no real-time visibility into which orphanages, homeless shelters, or community kitchens within a 5–10 km radius currently have nutritional demand and safe holding capacity. Potential recipients, conversely, have no awareness that surplus food is available only a few streets away.

### 2.3 Fragmented & Ad-Hoc Communication Channels
Conventional redistribution efforts rely on informal communication channels, such as telephone calls, SMS messages, and unstructured social media/WhatsApp groups. These channels suffer from:
- Lack of geographic filtering (broadcasts reach individuals too far away to collect in time).
- Absence of real-time inventory tracking (multiple charities may travel for the same donation).
- Complete lack of structured transactional state tracking.

### 2.4 Last-Mile Logistics Deficit
Non-profit recipient organizations (orphanages, elderly care homes, charitable trusts) operate under strict financial constraints and rarely possess dedicated transport vehicles, full-time drivers, or fuel budgets. Expecting recipient agencies to independently arrange rapid transportation creates an insurmountable operational bottleneck, causing timely donations to lapse.

### 2.5 Inconsistent & Vulnerable Claim Allocation
In informal donation networks, claims are settled arbitrarily or through chaotic group chats. Without formal concurrency control, race conditions occur where multiple recipients claim the same food item, or a single recipient over-claims beyond their actual beneficiary capacity, resulting in secondary food waste at the charity's facility.

### 2.6 Handover Verification Vulnerabilities & Fraud
Informal food handovers completely lack physical verification. A donor has no tamper-proof method to confirm whether the individual arriving at their loading dock is an authorized volunteer, an accredited charity representative, or an unauthorized opportunist. Unverified handovers lead to inventory discrepancies, phantom pickups, and severe safety liabilities.

### 2.7 Food Safety Liability & Regulatory Hesitation
Commercial food businesses are acutely aware of legal liabilities associated with foodborne illnesses. Without a standardized digital mechanism to record mandatory food safety self-declarations, preparation timestamps, and explicit terms of transfer, enterprise donors choose to destroy safe food rather than expose their brand to legal or regulatory jeopardy.

### 2.8 Absence of Traceability & Institutional Oversight
Charitable food redistribution often functions without administrative governance. In the absence of immutable audit logs, administrators cannot verify whether recipient organizations are legitimate accredited entities, monitor redistribution volumes, detect fraudulent activity, or provide regulatory reporting to food safety authorities.

---

## 3. Problem Statement Synthesis

> **Formal Problem Statement**:  
> *"Existing surplus food redistribution mechanisms in urban environments are characterized by acute temporal perishability, fragmented communication, lack of spatial proximity visibility, absent volunteer logistics coordination, unverified physical handovers, and legal hesitation surrounding food safety. There is a critical engineering need for an integrated, multi-stakeholder platform that combines server-side geospatial discovery, concurrency-safe claim allocation, dynamic volunteer dispatch, cryptographic handover authentication, and immutable auditability to facilitate safe, rapid, and transparent food rescue before perishability thresholds are reached."*
