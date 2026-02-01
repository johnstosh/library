/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Global cache of known free text URLs for books.
 * Uses pre-computed lookup results to avoid redundant provider queries.
 *
 * The cache stores normalized author names mapped to normalized titles,
 * which then map to space-separated URL strings.
 *
 * Normalization uses the same algorithms as {@link TitleMatcher}:
 * - Lowercase conversion
 * - Punctuation removal (except spaces)
 * - Trailing parenthetical content removal
 * - Leading article removal
 * - Multiple space collapse
 */
@Slf4j
public final class FreeTextLookupCache {

    /**
     * Map structure: normalizedAuthor -> (normalizedTitle -> space-separated URLs)
     */
    private static final Map<String, Map<String, String>> CACHE = new HashMap<>();

    static {
        // Data extracted from book-lookup-reference-backup/*.log HITs
        // Format: add(author, title, urls...)

        // Louisa May Alcott
        add("Louisa May Alcott", "Eight Cousins",
            "https://www.gutenberg.org/ebooks/2726",
            "https://archive.org/details/eightcousinsora04alcogoog",
            "https://librivox.org/eight-cousins-by-louisa-may-alcott/",
            "http://hdl.loc.gov/loc.gdc/scd0001.00021244107",
            "https://www.loc.gov/item/03020582/");

        add("Louisa May Alcott", "Little Women",
            "https://www.gutenberg.org/ebooks/37106",
            "https://archive.org/details/littlewomencompl0000loui",
            "https://librivox.org/little-women-by-louisa-may-alcott/",
            "https://www.loc.gov/item/14017126/");

        add("Louisa May Alcott", "An Old-Fashioned Girl",
            "https://archive.org/details/anoldfashionedg01alcogoog");

        // Robert Louis Stevenson
        add("Robert Louis Stevenson", "Kidnapped",
            "https://www.gutenberg.org/ebooks/421",
            "https://archive.org/details/kidnapped0000robe_m0o2",
            "https://librivox.org/kidnapped-by-robert-louis-stevenson/",
            "https://www.loc.gov/item/20005406/");

        add("Robert Louis Stevenson", "Treasure Island",
            "https://www.gutenberg.org/ebooks/120",
            "https://archive.org/details/treasureislandab0000unse_m5z9",
            "https://librivox.org/treasure-island-by-robert-louis-stevenson/",
            "https://www.loc.gov/item/50041964/");

        // Dante Alighieri
        add("Dante Alighieri", "The Purgatorio",
            "https://archive.org/details/purgatorio00dantuoft");

        add("Dante Alighieri", "Purgatory",
            "https://archive.org/details/cu31924012867424",
            "https://librivox.org/purgatory-by-rev-francois-xavier-schouppe/");

        add("Dante Alighieri", "Inferno",
            "https://www.loc.gov/item/16003562/");

        add("Dante Alighieri", "The Inferno",
            "https://www.loc.gov/item/16003562/");

        // Burton Egbert Stevenson
        add("Burton Egbert Stevenson", "Home Book of Verse for Young Folks",
            "https://archive.org/details/homebookversefo00stevgoog");

        add("Burton Egbert Stevenson", "The Home Book of Verse for Young Folks",
            "https://archive.org/details/homebookversefo00stevgoog");

        // John Bunyan
        add("John Bunyan", "Pilgrim's Progress",
            "https://archive.org/details/pilgrimsprogres00browgoog",
            "https://www.loc.gov/item/29022878/");

        // John of the Cross
        add("John of the Cross", "Living Flame of Love",
            "https://archive.org/details/johnofthecross00johnuoft");

        // Leo Tolstoy
        add("Leo Tolstoy", "Anna Karenina",
            "https://www.gutenberg.org/ebooks/1399",
            "https://archive.org/details/annakareninavolu0002leot_d7b9",
            "https://librivox.org/anna-karenina-book-1-by-leo-tolstoy/");

        // Dorothy Canfield Fisher
        add("Dorothy Canfield Fisher", "Understood Betsy",
            "https://www.gutenberg.org/ebooks/5347",
            "https://archive.org/details/understoodbetsy00fishgoog",
            "https://librivox.org/understood-betsy-by-dorothy-canfield-fisher-2/");

        // Margaret Sidney
        add("Margaret Sidney", "Five Little Peppers and How They Grew",
            "https://www.gutenberg.org/ebooks/2770",
            "https://archive.org/details/fivelittlepeppe00sidngoog");

        // Alban Butler
        add("Alban Butler", "Butler's Lives of the Saints",
            "https://archive.org/details/onehundredpious00butlgoog");

        // Ignatius of Loyola
        add("Ignatius of Loyola", "The Spiritual Exercises",
            "https://www.gutenberg.org/ebooks/70790",
            "https://archive.org/details/spiritualexercis0000igna_v4z4",
            "https://librivox.org/the-spiritual-exercises-by-st-ignatius-loyola/",
            "https://www.loc.gov/item/32011967/",
            "https://ccel.org/ccel/ignatius/exercises/exercises",
            "http://www.ccel.org/ccel/ignatius/exercises.html",
            "https://www.catholicplanet.com/ebooks/Spiritual-Exercises.pdf");

        // John Croiset
        add("John Croiset", "Devotion to the Sacred Heart of Jesus",
            "https://archive.org/details/devotiontosacred0000frjo",
            "https://www.loc.gov/item/ltf90012752/");

        // Frances Hodgson Burnett
        add("Frances Hodgson Burnett", "The Secret Garden",
            "https://www.gutenberg.org/ebooks/17396",
            "https://archive.org/details/secretgarden0000fran_n3s9",
            "https://librivox.org/the-secret-garden-by-frances-hodgson-burnett/");

        // C. A. Chardenal
        add("C. A. Chardenal", "First French Course or Rules and Exercises for Beginners",
            "https://archive.org/details/firstfrenchcours0000ccha");

        // Bess Streeter Aldrich
        add("Bess Streeter Aldrich", "A Lantern in Her Hand",
            "https://archive.org/details/lanterninherhand0000bess");

        // Gilbert Keith Chesterton
        add("Gilbert Keith Chesterton", "Orthodoxy",
            "https://www.gutenberg.org/ebooks/16769",
            "https://archive.org/details/orthodoxy0000gilb_y5p2",
            "https://librivox.org/orthodoxy-by-gk-chesterton/");

        add("Gilbert Keith Chesterton", "The Innocence of Father Brown",
            "https://www.gutenberg.org/ebooks/204");

        // Eleanor Estes
        add("Eleanor Estes", "Ginger Pye",
            "https://archive.org/details/gingerpyeodyssey0000elea");

        // Erich Maria Remarque
        add("Erich Maria Remarque", "All Quiet on the Western Front",
            "https://www.gutenberg.org/ebooks/75011",
            "https://archive.org/details/allquietonwester0000eric_f4c3");

        // John A. Lomax
        add("John A. Lomax", "American Ballads and Folk Songs",
            "https://archive.org/details/in.ernet.dli.2015.158045",
            "https://www.loc.gov/item/22021580/");

        // Charles Dickens (different name variations in logs)
        add("Charles John Huffam Dickens", "A Tale of Two Cities",
            "https://www.gutenberg.org/ebooks/98",
            "https://www.loc.gov/item/21015938/");

        add("Charles Dickens", "A Tale of Two Cities",
            "https://www.gutenberg.org/ebooks/98",
            "https://www.loc.gov/item/21015938/");

        // Better Homes and Gardens
        add("Better Homes and Gardens", "New Cook Book",
            "https://www.loc.gov/item/12001718/");

        // Louis of Granada
        add("Louis of Granada", "The Sinner's Guide",
            "https://www.loc.gov/item/39001478/",
            "https://www.ewtn.com/catholicism/library/sinners-guide-9832");

        // George MacDonald
        add("George MacDonald", "The Golden Key and Other Stories",
            "https://www.loc.gov/item/10011469/");

        // Claire Huchet Bishop
        add("Claire Huchet Bishop", "Lafayette",
            "https://www.loc.gov/item/22000701/");

        // Fulton J. Sheen
        add("Fulton J. Sheen", "Life of Christ",
            "https://www.loc.gov/item/13020319/");

        // Pope John Paul II
        add("Pope John Paul II", "Catechism of the Catholic Church",
            "https://www.loc.gov/item/99001094/");

        add("Pope John Paul II", "Catechism of the Catholic Church, c. 3",
            "https://www.loc.gov/item/99001094/");

        // Thomas Stearns Eliot
        add("Thomas Stearns Eliot", "Murder in the Cathedral",
            "https://www.loc.gov/item/musftpplaybills.200221187/");

        add("T. S. Eliot", "Murder in the Cathedral",
            "https://www.loc.gov/item/musftpplaybills.200221187/");

        // Oscar Wilde
        add("Oscar Fingal O'Flahertie Wills Wilde", "The Importance of Being Earnest",
            "https://www.gutenberg.org/ebooks/844");

        add("Oscar Wilde", "The Importance of Being Earnest",
            "https://www.gutenberg.org/ebooks/844");

        // Margery Williams
        add("Margery Williams", "The Velveteen Rabbit",
            "https://www.gutenberg.org/ebooks/11757",
            "https://librivox.org/the-velveteen-rabbit-by-margery-williams/");

        // Brother Lawrence
        add("Brother Lawrence", "The Practice of the Presence of God",
            "https://www.gutenberg.org/ebooks/5657",
            "https://ccel.org/ccel/lawrence/practice/practice");

        // Pope Francis
        add("Pope Francis", "Lumen Fidei",
            "https://www.vatican.va/content/francesco/en/encyclicals/documents/papa-francesco_20130629_enciclica-lumen-fidei.html");

        // Thomas Bertram Costain / Georg Ebers (LibriVox matched wrong author)
        add("Thomas Bertram Costain", "Joshua",
            "https://librivox.org/joshua-by-georg-ebers/");

        // Benedict Rohner
        add("Benedict Rohner", "The Life of the Blessed Virgin Mary",
            "https://www.catholicplanet.com/ebooks/Life-of-Blessed-Virgin-Mary.pdf");

        // Catholic Church - Enchiridion of Indulgences (user-requested addition)
        add("Catholic Church", "The Handbook of Indulgences",
            "https://www.vatican.va/roman_curia/tribunals/apost_penit/documents/rc_trib_appen_doc_20020826_enchiridion-indulgentiarum_lt.html");

        add("Catholic Church", "Enchiridion of Indulgences",
            "https://www.vatican.va/roman_curia/tribunals/apost_penit/documents/rc_trib_appen_doc_20020826_enchiridion-indulgentiarum_lt.html");

        add("Catholic Church", "The Handbook of Indulgences: Norms and Grants",
            "https://www.vatican.va/roman_curia/tribunals/apost_penit/documents/rc_trib_appen_doc_20020826_enchiridion-indulgentiarum_lt.html");

        // ===============================================
        // Books searched but not found (cached as empty)
        // These prevent redundant provider searches
        // ===============================================
        addNotFound("Lawrence G. Lovasik", "My Treasured Catholic Prayers");
        addNotFound("John Anthony Hardon", "The Catholic Lifetime Reading Plan");
        addNotFound("Marie McSwigan", "Snow Treasure");
        addNotFound("Robert J. Batastini", "Gather Comprehensive, c. 1");
        addNotFound("Robert J. Batastini", "Gather Comprehensive, c. 3");
        addNotFound("Robert J. Batastini", "Gather Comprehensive, c. 4");
        addNotFound("Robert J. Batastini", "Gather Comprehensive, c. 5");
        addNotFound("Ann Shields", "More of the Holy Spirit: How to Keep the Fire Burning in Our Hearts");
        addNotFound("Jean Bédard", "The Missing Violin");
        addNotFound("John Anthony Hardon", "The Catholic Catechism");
        addNotFound("Gabriel Denis", "The Reign of Jesus Through Mary");
        addNotFound("Richard Rolle", "The Enkindling of Love");
        addNotFound("Angela of Foligno", "Angela of Foligno: Complete Works");
        addNotFound("Jean-François Kieffer", "The Adventures of Loupio, Volume 2: The Hunters and Other Stories");
        addNotFound("Thomas Bertram Costain", "The Last Plantagenets");
        addNotFound("Alan Cooper", "The Inmates Are Running the Asylum");
        addNotFound("Ellery Queen", "Queen's Ransom");
        addNotFound("Warren Hasty Carroll", "The Cleaving of Christendom, A History of Christendom, Vol. 4");
        addNotFound("Eliyahu M. Goldratt", "The Haystack Syndrome: Sifting Information Out of the Data Ocean");
        addNotFound("John R. Wood", "Ordinary Lives, Extraordinary Mission");
        addNotFound("Elizabeth Hanna Pham", "A Storybook of Saints");
        addNotFound("Mary Ray", "Spring Tide");
        addNotFound("Michael Dubruiel", "Praying the Rosary: With the Joyful, Luminous, Sorrowful, & Glorious Mysteries");
        addNotFound("Karen West", "The Best of Polish Cooking");
        addNotFound("Philip D. Gallery", "Can You Find Jesus?: Introducing Your Child to the Gospel");
        addNotFound("Robyn Freedman Spizman", "The GIFTionary");
        addNotFound("Alfonso Maria de Liguori", "Way of the Cross at the National Shrine of The Divine Mercy, c. 3");
        addNotFound("Dola de Jong", "The Level Land");
        addNotFound("Antonio Michele Ghislieri", "The Order of the Mass with Prayers and Devotions");
        addNotFound("Paul Burns", "Butler's Lives of the Saints New Full Edition Supplement of New Saints and Blesseds Volume 1, c. 2");
        addNotFound("Margaret Rumer Godden", "The Kitchen Madonna");
        addNotFound("Peter V. Armenio", "Our Moral Life in Christ: A Complete Course");
        addNotFound("Ronda De Sola Chervin", "Quotable Saints");
        addNotFound("Abby Johnson", "Unplanned");
        addNotFound("Agnieszka Zawiska", "Historia o chłopcu imieniem Wojciech");
        addNotFound("Alan G. Konheim", "Cryptography: A Primer");
        addNotFound("Alan Schreck", "Catholic and Christian: An Explanation of Commonly Misunderstood Catholic Beliefs");
        addNotFound("Alan Schreck", "Catholic and Christian: An Explanation of Commonly Misunderstood Catholic Beliefs, Study Guide");
        addNotFound("Alban Goodier", "Saints for Sinners");
        addNotFound("Alfonso Maria de Liguori", "Way of the Cross at the National Shrine of The Divine Mercy, c. 1");
        addNotFound("Alfonso Maria de Liguori", "Way of the Cross at the National Shrine of The Divine Mercy, c. 2");
        addNotFound("Alphonsus Liguori", "The Road to Bethlehem: Daily Meditations for Advent and Christmas");
        addNotFound("Amadeus", "The Truth is Out There: Brendan & Erc in Exile Volume 1");
        addNotFound("Andrea J. Buchanan and Miriam Peskowitz", "The Pocket Daring Book for Girls");
        addNotFound("Andrew R. Maczynski", "Saint Stanislaus of Jesus and Mary Papczynski: The Life and Writings of the Marians' Founder");
        addNotFound("Anne Maybury", "The Brides of Bellenmore & Falcon's Shadow");
        addNotFound("Anne Pellowski", "Drawing Stories from Around the World and a Sampling of European Handkerchief Stories");
        addNotFound("Ann Rinaldi", "A Stitch in Time");
        addNotFound("Arnold Nesselrath", "Angels from the Vatican: The Invisible Made Visible");
        addNotFound("Arthur Catherall", "The Big Tusker");
        addNotFound("Arthur John Langguth", "Patriots: The Men Who Started the American Revolution");
        addNotFound("Barbara Willard", "If All the Swords in England: A Story of Thomas Becket");
        addNotFound("Benedictine Nun of Stanbrook", "Anne: The Life of Venerable Anne de Guigné");
        addNotFound("Benedict Joseph Groeschel", "Arise from Darkness");
        addNotFound("Bernice Wells Carlson", "The Right Play For You");
        addNotFound("Berry Fleming", "Colonel Effingham's Raid");
        addNotFound("Bess Streeter Aldrich", "The Lieutenant's Lady");
        addNotFound("Betty Crocker", "Betty Crocker's Christmas Cookbook");
        addNotFound("Brunor", "Saint Bernadette: The Miracle of Lourdes");
        addNotFound("Carl A. Anderson", "Our Lady of Guadalupe: Mother of the Civilization of Love");
        addNotFound("Carol Ryrie Brink", "Caddie Woodlawn");
        addNotFound("Catherine Drinker Bowen", "Miracle at Philadelphia");
        addNotFound("Charlotte Bronte & Emily Bronte", "The Complete Novels of Charlotte and Emily Brontë");
        addNotFound("Christopher Stefanick", "Absolute Relativism: The New Dictatorship And What To Do About It");
        addNotFound("Clarence Edward Elwell", "Our Goal and Our Guides");
        addNotFound("Clement Wood and Gloria Goddard", "The Complete Book of Games");
        addNotFound("Clive Staples Lewis", "The Lion, the Witch and the Wardrobe");
        addNotFound("Confraternity of Christian Doctrine", "New Testament of the New American Bible St. Joseph Edition");
        addNotFound("Daniel G. Amen", "The Brain Warrior's Way Cookbook");
        addNotFound("Daniel Kirk", "Snow Family");
        addNotFound("Dante Alighieri", "Paradise");
        addNotFound("Dante Alighieri", "The Paradiso");
        addNotFound("Diana M. Amadeo", "Holy Friends: Thirty Saints and Blesseds of the Americas");
        addNotFound("Dianne Ahern", "Today I Made My First Communion");
        addNotFound("Diego Paoletti", "Catherine Tekakwitha");
        addNotFound("Donald Senior", "The Catholic Study Bible");
        addNotFound("Doreen Cronin", "Giggle, Giggle, Quack");
        addNotFound("Dorothy Heiderstadt", "Marie Tanglehair");
        addNotFound("Edith Martha Almedingen", "Ellen");
        addNotFound("Edward Hays", "Prayers for the Domestic Church");
        addNotFound("Eleanore Myers Jewett", "The Hidden Treasure of Glaston");
        addNotFound("Eleanor Estes", "Pinky Pye");
        addNotFound("Elizabeth Enright", "Tatsinda");
        addNotFound("Elizabeth Ficocelli", "Lourdes: Font of Faith, Hope, and Charity");
        addNotFound("Elizabeth Jane Coatsworth", "Away Goes Sally");
        addNotFound("Elizabeth Kindelmann", "The Flame of Love");
        addNotFound("Eric Freeman", "Head First Design Patterns");
        addNotFound("Ethel C. Brill", "Madeleine Takes Command");
        addNotFound("Ethel Keating", "52 Fridays");
        addNotFound("Eva-Lis Wuorio", "To Fight in Silence");
        addNotFound("Eva-Lis Wuorio", "Venture at Midsummer");
        addNotFound("Francesco de Lucia", "St. Philomena Virgin and Martyr");
        addNotFound("Franciscan Friars of the Immaculate", "A Handbook on Guadalupe");
        addNotFound("François Jamart", "Complete Spiritual Doctrine of St. Therese of Lisieux");
        addNotFound("Frederick Schroeder", "Spiritual Direction for Today's Catholics");
        addNotFound("Fred Reinfeld", "Chess in a Nutshell");
        addNotFound("George Ferguson", "Signs and Symbols in Christian Art");
        addNotFound("George Weigel", "Letters to a Young Catholic");
        addNotFound("Gilbert Keith Chesterton", "The Wisdom of Mr. Chesterton");
        addNotFound("Giuliana Cavallini", "Saint Martin de Porres: Apostle of Charity");
        addNotFound("Guido Visconti", "Clare and Francis");
        addNotFound("Harriett Mulford Stone Lothrop", "Five Little Peppers at School");
        addNotFound("Harvey Hirsch & Audrey Hirsch", "The Crèche of Krakow: A Christmas Story");
        addNotFound("Henry Graham Greene", "Our Man in Havana");
        addNotFound("Henryk Sienkiewicz", "Tales from Henryk Sienkiewicz");
        addNotFound("Herbert Norman Schwarzkopf Jr.", "It Doesn't Take a Hero");
        addNotFound("Hugh F. Blunt", "Listen, Mother of God!: Thoughts on the Litany of Loreto");
        addNotFound("Hugo Hoever", "Illustrated Lives of the Saints");
        addNotFound("International Commission on English in the Liturgy", "The Divine Office Hymnal");
        addNotFound("Jacques Philippe", "Following the Holy Spirit");
        addNotFound("James Herriot", "James Herriot's Treasury for Children");
        addNotFound("James Socias", "Introduction To Catholicism: A Complete Course");
        addNotFound("James Socias", "The Sacraments: Source of Our Life in Christ");
        addNotFound("Jan Ormerod", "101 Things to Do with a Baby, c. 1");
        addNotFound("Jan Ormerod", "101 Things to Do with Baby, c. 2");
        addNotFound("Jason Evert", "Pure Faith: A Prayer Book for Teens");
        addNotFound("Jay L. Wile", "Exploring Creation with Biology");
        addNotFound("Jay L. Wile", "Exploring Creation with Chemistry");
        addNotFound("Jean Bothwell", "The Promise of the Rose");
        addNotFound("Jean Craighead George", "Frightful's Mountain");
        addNotFound("Jean Craighead George", "My Side of the Mountain");
        addNotFound("Jean Craighead George", "On the Far Side of the Mountain");
        addNotFound("Jean-François Kieffer", "The Adventures of Loupio, Volume 1: The Encounter and other stories");
        addNotFound("Jean-François Kieffer", "The Adventures of Loupio, Volume 3: The Tournament");
        addNotFound("Jean Merrill", "The Pushcart War");
        addNotFound("Jeanne Perego", "Joseph and Chico: The Life of Pope Benedict XVI as Told by a Cat");
        addNotFound("Jennie Bishop", "The Squire and the Scroll");
        addNotFound("Jenny Overton", "The Nightwatch Winter");
        addNotFound("Jenny Overton", "The Ship from Simnel Street");
        addNotFound("Jeron Ashford", "Winter Candle");
        addNotFound("Jimmy Akin", "Teaching with Authority");
        addNotFound("Jiří Adámek", "Foundations of Coding: Theory and Applications of Error-Correcting Codes with an Introduction to Cryptography and Information Theory");
        addNotFound("John Bergsma", "Bible Basics for Catholics: A New Picture of Salvation History");
        addNotFound("John Doe", "The Social Doctrine of the Catholic Church");
        addNotFound("John Hawkesworth", "Upstairs, Downstairs II: In My Lady's Chamber");
        addNotFound("Johnny Doherty", "Ask Search Knock");
        addNotFound("Josefa Menéndez", "The Way of Divine Love");
        addNotFound("Josemaría Escrivá", "Children of God");
        addNotFound("Joseph Aloisius Ratzinger", "A School of Prayer: The Saints Show Us How to Pray");
        addNotFound("Joseph Aloisius Ratzinger", "Friendship with Jesus: Pope Benedict XVI Speaks to Children on Their First Holy Communion");
        addNotFound("Joseph Bernardin", "The Gift of Peace");
        addNotFound("Joseph Ignatius Dirvin", "Saint Catherine Labouré of the Miraculous Medal");
        addNotFound("Joseph Ratzinger", "Compendium of the Catechism of the Catholic Church");
        addNotFound("Karen Garver Santorum", "Everyday Graces: A Child's Book of Good Manners");
        addNotFound("Karol Józef Wojtyła", "Crossing the Threshold of Hope");
        addNotFound("Karol Józef Wojtyła", "Mary: God's Yes to Man");
        addNotFound("Karol Józef Wojtyła", "Rise, Let Us Be on Our Way");
        addNotFound("Kate Morton", "The Lake House");
        addNotFound("Katherine Larson", "Today I Was Baptized");
        addNotFound("Katherine Paterson", "The Sign of the Chrysanthemum");
        addNotFound("Kathleen Norris", "The Holy Twins: Benedict and Scholastica");
        addNotFound("Kenneth G. Libbrecht", "The Art of the Snowflake: A Photographic Album");
        addNotFound("Kurt Poterack", "The Adoremus Hymnal");
        addNotFound("Laura Lee Hope", "The Bobbsey Twins In Echo Valley");
        addNotFound("Laurel Porter-Gaylord", "I Love My Daddy Because...");
        addNotFound("Lawrence George Lovasik", "The New Saint Joseph First Communion Catechism");
        addNotFound("Libby Hathorn", "Thunderwith");
        addNotFound("Lois Lenski", "Cotton in My Sack");
        addNotFound("Lois Lenski", "Judy's Journey");
        addNotFound("Lois Lenski", "Strawberry Girl");
        addNotFound("Lois Lowry", "Number the Stars");
        addNotFound("Louisa May Alcott", "A Round Dozen");
        addNotFound("Mai Leung", "The Classic Chinese Cook Book");
        addNotFound("Margaret Mayo", "Brother Sun, Sister Moon: The Life and Stories of St. Francis");
        addNotFound("Margaret Rumer Godden", "The Rocking-Horse Secret");
        addNotFound("Margi Preus", "Heart of a Samurai");
        addNotFound("Margot Benary-Isbert", "The Long Way Home");
        addNotFound("Marie-Geneviève Roux and Élisabeth Charpy", "Saint Catherine Labouré");
        addNotFound("Marilyn M. Shannon", "Fertility, Cycles and Nutrition");
        addNotFound("Marjorie Kinnan Rawlings", "The Yearling");
        addNotFound("Martha Grimes", "The Lamorna Wink");
        addNotFound("Mary Kathleen Glavich", "Saint Julie Billiart: The Smiling Saint");
        addNotFound("Mary Kathleen Glavich", "Saint Thérèse of Lisieux: The Way of Love");
        addNotFound("Mary Noel Streatfeild", "Ballet Shoes");
        addNotFound("Mary Noel Streatfeild", "When the Sirens Wailed");
        addNotFound("Matthew Kelly", "Holy Moments: A Handbook for the Rest of Your Life");
        addNotFound("Matthew Kelly", "Rediscover Catholicism");
        addNotFound("Matthew Kelly", "Rediscover the Saints");
        addNotFound("Matthew Kelly", "Resisting Happiness");
        addNotFound("Matthew Kelly", "The Four Signs of a Dynamic Catholic");
        addNotFound("Max Lucado", "Punchinello and the Most Marvelous Gift");
        addNotFound("Meindert DeJong", "Hurry Home, Candy");
        addNotFound("Meindert DeJong", "Journey from Peppermint Street");
        addNotFound("Meindert DeJong", "Shadrach");
        addNotFound("Michael A. McGuire", "The New Baltimore Catechism and Mass No. 2");
        addNotFound("Michael David O'Brien", "A Landscape with Dragons: The Battle for Your Child's Mind");
        addNotFound("Michael E. Giesler", "Grain of Wheat");
        addNotFound("Mike Aquilina", "Understanding the Mass: 100 Questions, 100 Answers");
        addNotFound("Mildred Delois Taylor", "Roll of Thunder, Hear My Cry");
        addNotFound("Neil Buchanan Winston", "Code: Polonaise");
        addNotFound("Nelle Harper Lee", "To Kill a Mockingbird");
        addNotFound("Pamela Jane", "Noelle of the Nutcracker");
        addNotFound("Patricia Corbin", "All About Wicker");
        addNotFound("Patricia Treece", "Meet Padre Pio");
        addNotFound("Paul Burns", "Butler's Lives of the Saints New Full Edition Supplement of New Saints and Blesseds Volume 1, c. 1");
        addNotFound("Paul Burns", "Butler's Lives of the Saints New Full Edition Supplement of New Saints and Blesseds Volume 1, c. 3");
        addNotFound("Pauline Pears", "Rodale's Illustrated Encyclopedia of Organic Gardening");
        addNotFound("Peter Kurzdorfer", "The Everything Chess Basics Book");
        addNotFound("Peter V. Armenio", "The Mystery of Redemption and Christian Discipleship");
        addNotFound("Philip D. Gallery", "Can You Find Saints?");
        addNotFound("Phyllis McGinley", "Saint-Watching");
        addNotFound("Pope Francis", "Laudato Si': On Care for Our Common Home");
        addNotFound("Pope Francis", "The Joy of the Gospel");
        addNotFound("Pope John Paul II", "The Lay Members of Christ's Faithful People");
        addNotFound("Raphael Brown", "The Life of Mary as Seen by the Mystics");
        addNotFound("Raymond Arroyo", "Mother Angelica: The Remarkable Story of a Nun, Her Nerve, and a Network of Miracles");
        addNotFound("Reader's Digest Editors", "Fireside Reader");
        addNotFound("Reginald Garrigou-Lagrange", "The Three Ages of the Interior Life");
        addNotFound("Richard Brookhiser", "Founding Father: Rediscovering George Washington");
        addNotFound("Robert J. Batastini", "Gather Comprehensive, c. 2");
        addNotFound("Robert J. Batastini", "Gather Comprehensive, c. 6");
        addNotFound("Robert J. Batastini", "Gather Comprehensive, c. 7");
        addNotFound("Robert J. Batastini", "Gather Comprehensive, c. 8");
        addNotFound("Robert Lee Frost", "The Poetry of Robert Frost");
        addNotFound("Roger Burke Dooley", "Gone Tomorrow");
        addNotFound("Roger Fisher", "Getting to Yes: Negotiating Agreement Without Giving In");
        addNotFound("Samuel McBratney", "Guess How Much I Love You");
        addNotFound("Scott Walker Hahn", "Joy to the World: How Christ's Coming Changed Everything (and Still Does)");
        addNotFound("Sean Covey", "The 7 Habits of Highly Effective Teens");
        addNotFound("Sergio Cariello", "The Action Bible: God's Redemptive Story");
        addNotFound("Servais-Théodore Pinckaers", "Morality: The Catholic View");
        addNotFound("Shawn D. Carney", "To the Heart of the Matter: The 40-Day Companion to Live a Culture of Life");
        addNotFound("Sheldon Allan Silverstein", "The Giving Tree");
        addNotFound("Sister Cecilia, Sister John Joseph, Sister Rose Margaret", "We Sing of Our World");
        addNotFound("Sister Marie of St. Peter", "Little Manual of the Confraternity of the Holy Face");
        addNotFound("Sister Mary", "The Catholic Mother's Helper in Training Her Children");
        addNotFound("Sisters of Charity of Our Lady, Mother of the Church", "Favorite Novenas to the Holy Spirit");
        addNotFound("Slaves of the Immaculate Heart of Mary", "Saints to Know and Love");
        addNotFound("Stephen K. Ray", "Crossing the Tiber");
        addNotFound("Steve Krug", "Don't Make Me Think");
        addNotFound("Stewart L. Tubbs", "A Systems Approach to Small Group Interaction");
        addNotFound("Susan Helen Wallace", "Saints for Young Readers for Every Day Volume 1: January - June");
        addNotFound("Susan Wallace", "Saints for Young Readers for Every Day Volume 2 July - December");
        addNotFound("Sydney Taylor", "All-of-a-Kind Family");
        addNotFound("Sydney Taylor", "All-Of-A-Kind Family Downtown");
        addNotFound("Tatsuya Endo", "Spy × Family, v. 2");
        addNotFound("Tatsuya Endo", "Spy × Family, v. 3");
        addNotFound("Tatsuya Endo", "Spy × Family, v.4");
        addNotFound("Tatsuya Endo", "Spy × Family, v. 5");
        addNotFound("Theodor Seuss Geisel", "Dr. Seuss's Sleep Book");
        addNotFound("Theodor Seuss Geisel", "Horton Hatches the Egg");
        addNotFound("Theodor Seuss Geisel", "Horton Hears a Who!");
        addNotFound("Theodor Seuss Geisel", "How the Grinch Stole Christmas!");
        addNotFound("Theodor Seuss Geisel", "Yertle the Turtle and Other Stories");
        addNotFound("Thomas Colin Campbell", "The China Study");
        addNotFound("Thomas Dubay", "Seeking Spiritual Direction: How to Grow the Divine Life Within");
        addNotFound("Thomas M. Doran", "Toward the Gleam");
        addNotFound("Thomas Michael Bond", "Paddington at Large");
        addNotFound("Timothy M. Gallagher", "Begin Again: The Life and Spiritual Legacy of Bruno Lanteri");
        addNotFound("Timothy M. Gallagher", "The Discernment of Spirits: An Ignatian Guide for Everyday Living");
        addNotFound("Toni Pagotto", "Pope John Paul II");
        addNotFound("United States Conference of Catholic Bishops", "United States Catholic Catechism for Adults");
        addNotFound("Various", "New American Bible");
        addNotFound("Warren Hasty Carroll", "The Founding of Christendom, A History of Christendom Vol. 1");
        addNotFound("Waverly House Ltd", "The Busy Day Book");
        addNotFound("William J. Bennett", "The Book of Virtues: A Treasury of Great Moral Stories");
        addNotFound("William J. Bennett", "The Children's Book of Faith");
        addNotFound("William John Bennett", "The Children's Book of Virtues");

        log.info("FreeTextLookupCache initialized with {} authors", CACHE.size());
    }

    private FreeTextLookupCache() {
        // Utility class
    }

    /**
     * Add an entry to the cache with multiple URLs.
     */
    private static void add(String author, String title, String... urls) {
        String normalizedAuthor = normalizeAuthor(author);
        String normalizedTitle = normalizeTitle(title);
        String urlString = String.join(" ", urls);

        CACHE.computeIfAbsent(normalizedAuthor, k -> new HashMap<>())
             .put(normalizedTitle, urlString);
    }

    /**
     * Mark a book as searched but not found (cache empty string).
     * This prevents redundant provider searches.
     */
    private static void addNotFound(String author, String title) {
        add(author, title); // varargs with no args = empty string
    }

    /**
     * Look up cached URLs for a book by author and title.
     * Uses normalized matching based on TitleMatcher algorithms.
     *
     * @param author the author name (can be null)
     * @param title the book title
     * @return space-separated URLs if found, null otherwise
     */
    public static String lookup(String author, String title) {
        if (title == null || title.isBlank()) {
            return null;
        }

        String normalizedTitle = normalizeTitle(title);

        // If author is provided, try exact author match first
        if (author != null && !author.isBlank()) {
            String normalizedAuthor = normalizeAuthor(author);

            Map<String, String> authorCache = CACHE.get(normalizedAuthor);
            if (authorCache != null) {
                String urls = authorCache.get(normalizedTitle);
                if (urls != null) {
                    log.debug("Cache hit for author='{}' title='{}' (exact)", author, title);
                    return urls;
                }
            }

            // Try fuzzy author matching (last name match)
            String authorLastName = getLastName(normalizedAuthor);
            for (Map.Entry<String, Map<String, String>> entry : CACHE.entrySet()) {
                String cachedLastName = getLastName(entry.getKey());
                if (cachedLastName.equals(authorLastName)) {
                    String urls = entry.getValue().get(normalizedTitle);
                    if (urls != null) {
                        log.debug("Cache hit for author='{}' title='{}' (last name match)", author, title);
                        return urls;
                    }
                }
            }
        }

        // Fall back to title-only search across all authors
        for (Map<String, String> authorCache : CACHE.values()) {
            String urls = authorCache.get(normalizedTitle);
            if (urls != null) {
                log.debug("Cache hit for title='{}' (title-only)", title);
                return urls;
            }
        }

        return null;
    }

    /**
     * Get the first URL from a cached result.
     *
     * @param author the author name (can be null)
     * @param title the book title
     * @return first URL if found, null otherwise
     */
    public static String lookupFirstUrl(String author, String title) {
        String urls = lookup(author, title);
        if (urls == null) {
            return null;
        }
        int spaceIndex = urls.indexOf(' ');
        return spaceIndex > 0 ? urls.substring(0, spaceIndex) : urls;
    }

    /**
     * Normalize a title for cache lookup using TitleMatcher-style normalization.
     * - Convert to lowercase
     * - Remove trailing parenthetical content
     * - Remove leading articles
     * - Remove punctuation except spaces
     * - Collapse multiple spaces
     */
    static String normalizeTitle(String title) {
        return title.toLowerCase()
                .replaceAll("\\s*\\([^)]*\\)\\s*$", "") // Remove trailing (date), (edition), etc.
                .replaceAll("^(the|a|an)\\s+", "")
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Normalize an author name for cache lookup.
     * - Convert to lowercase
     * - Remove punctuation except spaces
     * - Collapse multiple spaces
     */
    static String normalizeAuthor(String author) {
        return author.toLowerCase()
                .replaceAll("[^a-z\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Extract the last name from a normalized author string.
     * Handles both "First Last" and "Last, First" formats.
     */
    private static String getLastName(String author) {
        if (author.contains(",")) {
            return author.split(",")[0].trim();
        }
        String[] parts = author.split("\\s+");
        return parts.length > 0 ? parts[parts.length - 1] : author;
    }

    /**
     * Get cache statistics for monitoring.
     *
     * @return number of unique authors in cache
     */
    public static int getAuthorCount() {
        return CACHE.size();
    }

    /**
     * Get total number of cached book entries.
     *
     * @return total number of author/title combinations
     */
    public static int getBookCount() {
        return CACHE.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
}
