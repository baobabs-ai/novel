export interface WenkuNovelOutlineDto {
  id: string;
  title: string;
  titleEn: string;
  cover: string;
  favored?: string;
}

export interface WenkuNovelDto {
  title: string;
  titleEn: string;
  cover?: string;
  authors: string[];
  artists: string[];
  keywords: string[];
  publisher?: string;
  imprint?: string;
  latestPublishAt?: number;
  level: 'For All Ages' | 'For Adults' | 'Serious';
  introduction: string;
  webIds: string[];
  volumes: WenkuVolumeDto[];
  glossary: { [key: string]: string };
  visited: number;
  favored?: string;
  volumeEn: string[];
  volumeJp: VolumeJpDto[];
}

export interface WenkuVolumeDto {
  asin: string;
  title: string;
  titleEn?: string;
  cover: string;
  coverHires?: string;
  publisher?: string;
  imprint?: string;
  publishAt?: number;
}

export interface VolumeJpDto {
  volumeId: string;
  total: number;
  baidu: number;
  youdao: number;
  gpt: number;
  sakura: number;
}

export interface AmazonNovel {
  title: string;
  titleEn?: string;
  r18: boolean;
  authors: string[];
  artists: string[];
  introduction: string;
  volumes: Array<WenkuVolumeDto>;
}

type PresetKeywordsGroup = {
  title: string;
  presetKeywords: string[];
};

type PresetKeywordsExplanation = {
  word: string;
  explanation: string;
};

type PresetKeywords = {
  groups: PresetKeywordsGroup[];
  explanations: PresetKeywordsExplanation[];
};

const groupsNonR18: PresetKeywordsGroup[] = [
  {
    title: 'Perspective',
    presetKeywords: ['Male Lead', 'Female Lead', 'TS', 'Ensemble Cast'],
  },
  {
    title: 'Characters',
    presetKeywords: [
      'Childhood Friend',
      'Brother-Sister',
      'Sister-Brother',
      'Parent-Child',
      'Teacher-Student',
      'Loli',
      'Non-human',
      'Trap',
      'Long Aotian',
      'Tsundere',
      'Yandere',
      'Villainess',
    ],
  },
  {
    title: 'World',
    presetKeywords: [
      'Modern',
      'Sci-Fi',
      'Fantasy',
      'Historical',
      'Apocalypse',
      'School',
      'Game',
      'Workplace',
      'Chinese Style',
      'Japanese Style',
    ],
  },
  {
    title: 'Atmosphere',
    presetKeywords: ['Healing', 'Happy', 'Twisted', 'Cruel', 'Depressing', 'Bizarre', 'Suspense'],
  },
  {
    title: 'Theme',
    presetKeywords: [
      'Pure Love',
      'Harem',
      'Reverse Harem',
      'Yuri',
      'BL',
      'NTR',
      'Battle',
      'Adventure',
      'Supernatural Powers',
      'Mecha',
      'War',
      'Management',
      'Slice of Life',
      'Mystery',
      'Competition',
      'Travel',
      'Transmigration',
      'Revenge',
      'Misunderstanding',
      'Deserved it',
    ],
  },
  {
    title: 'Other',
    presetKeywords: ['Anime Adaptation', 'Manga Adaptation', 'Spinoff'],
  },
];

const explanationsNonR18: PresetKeywordsExplanation[] = [
  {
    word: 'Male Lead, Female Lead, TS, Ensemble Cast',
    explanation:
      'The main perspective of the novel, in most cases, only one is selected. Do not add "Ensemble Cast" for simple dual protagonists.',
  },
  {
    word: 'Long Aotian',
    explanation: 'Regardless of gender, but it must be the main perspective.',
  },
  {
    word: 'Sci-Fi',
    explanation: 'Sci-fi style worldview, such as near-future Earth, fantasy science fiction otherworld, and space.',
  },
  {
    word: 'Fantasy',
    explanation: 'Fantasy style worldview, such as a common otherworld.',
  },
  {
    word: 'Game',
    explanation:
      'The main venue of the novel is in a game, including real games and transmigration to a game world. Note that having a status panel does not mean it is a game world.',
  },
  {
    word: 'Healing',
    explanation: '"Healing" means the plot is light, such as a slow life series.',
  },
  {
    word: 'Twisted, Cruel, Depressing, Bizarre',
    explanation:
      '"Twisted" indicates a plot with emotional entanglement, simple love triangles do not count. "Cruel" indicates a dark setting or plot, such as a death game or battle royale. "Depressing" indicates a plot that makes people depressed, note that depressing does not necessarily mean character death. "Bizarre" indicates heavy or bloody descriptions.',
  },
  {
    word: 'Harem, Reverse Harem, Yuri, BL',
    explanation: 'These tags are all interpreted broadly. Pseudo-harem and pseudo-yuri can be used.',
  },
  {
    word: 'Transmigration',
    explanation: 'Both reincarnation and transmigration can use this tag.',
  },
  {
    word: 'Anime Adaptation, Manga Adaptation, Spinoff',
    explanation:
      'Only when the novel is the main body can the "Anime Adaptation" and "Manga Adaptation" tags be added. If the main body is another type of work, please add "Spinoff".',
  },
];

export const presetKeywordsNonR18: PresetKeywords = {
  groups: groupsNonR18,
  explanations: explanationsNonR18,
};

const groupsR18: PresetKeywordsGroup[] = [];

const explanationsR18: PresetKeywordsExplanation[] = [];

export const presetKeywordsR18: PresetKeywords = {
  groups: groupsR18,
  explanations: explanationsR18,
};
